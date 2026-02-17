@file:OptIn(ExperimentalContracts::class)

package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.DetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Hendelse
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.HendelseMetadata
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.HendelseMetadataTidspunktFraKilde
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.HendelseMetadataUtfoertAv
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Snapshot
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Tilstand
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.TilstandApiKall
import no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseRecordRepository
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.StoredHendelseRecord
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class IngenArbeidssoekerIdFunnet(message: String) : RuntimeException(message)
class IngenHendelserFunnet(message: String) : RuntimeException(message)
class FantIkkeIdentitetsnummer(message: String) : RuntimeException(message)
class UgyldigIdentFormat(message: String) : RuntimeException(message)

class BrukerstoetteService(
    private val consumerVersion: Int,
    private val kafkaKeysClient: KafkaKeysClient,
    private val oppslagApiClient: OppslagApiClient,
    private val hendelseRecordRepository: HendelseRecordRepository
) {
    private val errorLogger = LoggerFactory.getLogger("error_logger")
    private val apiOppslagLogger = LoggerFactory.getLogger("api_oppslag_logger")

    suspend fun hentDetaljer(id: String): DetaljerResponse {

        val identitetsnummer = when {
            id.matches(Regex("\\d{11}")) -> id
            else -> {
                val periodeId = runCatching { UUID.fromString(id) }
                    .getOrElse { throw UgyldigIdentFormat("Ugyldig identformat") }
                hendelseRecordRepository.hentIdentitetsnummerForPeriodeId(periodeId)
                    ?: throw FantIkkeIdentitetsnummer("Fant ikke identitetsnummer for periodeId: $periodeId")
            }
        }

        val (id, key) = kafkaKeysClient.getIdAndKeyOrNull(identitetsnummer)
            ?: throw IngenArbeidssoekerIdFunnet("Fant ikke arbeidssøkerId")

        val hendelser = hendelseRecordRepository.readAllNestedRecordsForId(
            consumerVersion,
            id,
            merged = false
        )
        if (hendelser.isEmpty()) throw IngenHendelserFunnet("Fant ingen hendelser for person")

        val sistePeriode = sistePeriode(hendelser)
        val innkommendeHendelse = historiskeTilstander(hendelser).toList()
        val fraOppslagsApi: List<ApiData> = hentFraOppslagsApi(identitetsnummer)
            .fold(
                { error ->
                    errorLogger.error(
                        "Feil ved henting av opplysninger fra oppslagsapi: {}:{}",
                        error.httpCode,
                        error.message
                    )
                    emptyList()
                },
                { it }
            )

        val partition = hendelser.filterNot { it.merged }.firstOrNull()?.partition
        return DetaljerResponse(
            recordKey = key,
            kafkaPartition = partition,
            historikk = innkommendeHendelse.map { snapshot ->
                snapshot.copy(
                    nyTilstand = snapshot.nyTilstand?.let { enrich(it, fraOppslagsApi) },
                    gjeldeneTilstand = snapshot.gjeldeneTilstand?.let { enrich(it, fraOppslagsApi) }
                )
            },
            arbeidssoekerId = id,
            gjeldeneTilstand = sistePeriode?.let { enrich(it, fraOppslagsApi) }
        )
    }

    private suspend fun hentFraOppslagsApi(identitetsnummer: String): Either<Error, List<ApiData>> {
        return either {
            val perioder =
                oppslagApiClient.perioder(identitetsnummer).bind().map(ArbeidssoekerperiodeResponse::periodeId)
            val opplysninger =
                perioder.flatMap { periodeId ->
                    oppslagApiClient.opplysninger(identitetsnummer, periodeId).bind()
                        .map { opplysning -> ApiData(periodeId, opplysning.opplysningerOmArbeidssoekerId, null) }
                        .let { it.ifEmpty { listOf(ApiData(periodeId, null, null)) } }
                }
            val profileringer = perioder.flatMap { periodeId ->
                oppslagApiClient.profileringer(identitetsnummer, periodeId).bind()
                    .map { ApiData(periodeId, it.opplysningerOmArbeidssoekerId, it.profileringId) }
            }
            return perioder.map { periodeId -> ApiData(periodeId, null, null) }
                .filterNot { periode -> opplysninger.any { periode.periodeId == it.periodeId } }
                .plus(opplysninger.filterNot { opplysning -> profileringer.any { profilering -> profilering.periodeId == opplysning.periodeId } })
                .right()
                .onRight { result ->
                    apiOppslagLogger.info(
                        "Hentet data fra oppslagsapi, perioder: {}, opplysninger: {}, profileringer: {}",
                        result.distinctBy { it.periodeId }.size,
                        result.distinctBy { it.opplysningsId }.size,
                        result.distinctBy { it.profileringsId }.size
                    )
                }
        }
    }
}

fun enrich(tilstand: Tilstand, apiData: List<ApiData>): Tilstand {
    val periodeData = apiData.filter { it.periodeId == tilstand.periodeId }
    val harPeriode = periodeData.isNotEmpty()
    val harOpplysning = tilstand.gjeldeneOpplysningsId?.let { opplysningId ->
        periodeData.any { it.opplysningsId == opplysningId }
    } ?: false
    val harProfilering = tilstand.gjeldeneOpplysningsId?.let { opplysningId ->
        periodeData.any { it.opplysningsId == opplysningId && it.profileringsId != null }
    } ?: false
    return tilstand.copy(
        apiKall = TilstandApiKall(
            harPeriode = harPeriode,
            harOpplysning = harOpplysning,
            harProfilering = harProfilering
        )
    )
}

data class ApiData(
    val periodeId: UUID,
    val opplysningsId: UUID?,
    val profileringsId: UUID?,
)

fun sistePeriode(hendelser: List<StoredHendelseRecord>): Tilstand? =
    hendelser
        .filterNot { it.merged }
        .sortedBy { it.offset }
        .fold(null as Tilstand?, ::beregnTilstand)

fun historiskeTilstander(hendelser: List<StoredHendelseRecord>): Iterable<Snapshot> =
    hendelser.map(null as Tilstand?) { tilstand, hendelse ->
        val nyTilstand = beregnTilstand(tilstand, hendelse)
        val resultat = Snapshot(
            endret = nyTilstand !== tilstand,
            hendelse = Hendelse(
                hendelseId = hendelse.data.hendelseId,
                hendelseType = hendelse.data.hendelseType,
                merged = hendelse.merged,
                kafkaPartition = hendelse.partition,
                kafkaOffset = hendelse.offset,
                metadata = HendelseMetadata(
                    tidspunkt = hendelse.data.metadata.tidspunkt,
                    utfoertAv = HendelseMetadataUtfoertAv(
                        type = hendelse.data.metadata.utfoertAv.type.name,
                        id = hendelse.data.metadata.utfoertAv.id
                    ),
                    kilde = hendelse.data.metadata.kilde,
                    aarsak = hendelse.data.metadata.aarsak,
                    tidspunktFraKilde = hendelse.data.metadata.tidspunktFraKilde?.let {
                        HendelseMetadataTidspunktFraKilde(
                            tidspunkt = it.tidspunkt,
                            avvikstype = it.avviksType.name
                        )
                    },
                ),
                data = hendelse.data,
                api = null,
                traceparent = hendelse.traceparent
            ),
            gjeldeneTilstand = tilstand,
            nyTilstand = nyTilstand
        )
        Pair(nyTilstand, resultat)
    }

fun <V, S, R> Iterable<V>.map(initial: S, function: (S, V) -> Pair<S, R>): Iterable<R> {
    var current = initial
    return map { value ->
        val (newState, result) = function(current, value)
        current = newState
        result
    }
}

fun beregnTilstand(tilstand: Tilstand?, hendelse: StoredHendelseRecord): Tilstand? =
    when {
        !tilstand.harAktivPeriode() && hendelse.erStartet() -> {
            Tilstand(
                harAktivePeriode = true,
                startet = hendelse.data.metadata.tidspunkt,
                avsluttet = null,
                harOpplysningerMottattHendelse = false,
                apiKall = null,
                periodeId = hendelse.data.hendelseId
            )
        }

        !tilstand.harAktivPeriode() -> tilstand
        tilstand.harAktivPeriode() && hendelse.erOpplysningerMottatt() -> {
            tilstand.copy(
                harOpplysningerMottattHendelse = true,
                gjeldeneOpplysningsId = (hendelse.data as OpplysningerOmArbeidssoekerMottatt)
                    .opplysningerOmArbeidssoeker.id
            )
        }

        tilstand.harAktivPeriode() && hendelse.erAvsluttet() -> {
            tilstand.copy(
                avsluttet = hendelse.data.metadata.tidspunkt,
                harAktivePeriode = false
            )
        }

        else -> tilstand
    }

fun StoredHendelseRecord.erAvsluttet(): Boolean = this.data is Avsluttet
fun StoredHendelseRecord.erStartet(): Boolean = this.data is Startet
fun StoredHendelseRecord.erOpplysningerMottatt(): Boolean = this.data is OpplysningerOmArbeidssoekerMottatt

fun Tilstand?.harAktivPeriode(): Boolean {
    contract {
        returns(true) implies (this@harAktivPeriode != null)
    }
    return this != null && this.avsluttet == null
}
