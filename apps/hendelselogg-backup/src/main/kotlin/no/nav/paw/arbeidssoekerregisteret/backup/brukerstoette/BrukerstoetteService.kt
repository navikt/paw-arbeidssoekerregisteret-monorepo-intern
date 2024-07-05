@file:OptIn(ExperimentalContracts::class)

package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.*
import no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.backup.database.readAllRecordsForId
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.vo.StoredData
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class BrukerstoetteService(
    private val oppslagAPI: OppslagApiClient,
    private val kafkaKeysClient: KafkaKeysClient,
    private val applicationContext: ApplicationContext,
    private val hendelseDeserializer: HendelseDeserializer
) {
    private val errorLogger = LoggerFactory.getLogger("error_logger")
    private val apiOppslagLogger = LoggerFactory.getLogger("api_oppslag_logger")
    suspend fun hentDetaljer(identitetsnummer: String): DetaljerResponse? {
        val (id, _) = kafkaKeysClient.getIdAndKey(identitetsnummer)
        val hendelser = transaction {
            (::readAllRecordsForId)(hendelseDeserializer, applicationContext, id)
        }
        if (hendelser.isEmpty()) {
            return null
        } else {
            val sistePeriode = sistePeriode(hendelser)
            val innkommendeHendelse = historiskeTilstander(hendelser).toList()
            val fraOppslagsApi = hentFraOppslagsApi(identitetsnummer)
                .fold(
                    { error ->
                        errorLogger.error("Feil ved henting av opplysninger fra oppslagsapi: {}:{}", error.httpCode, error.message)
                        emptyList()
                    },
                    { it }
                )

            val partition = hendelser.firstOrNull()?.partition
            return DetaljerResponse(
                recordKey = hendelser.first().recordKey,
                kafkaPartition = partition,
                historikk = innkommendeHendelse.map { snapshot ->
                    snapshot.copy(
                        nyTilstand = snapshot.nyTilstand?.let { enrich(it, fraOppslagsApi) },
                        gjeldeneTilstand = snapshot.gjeldeneTilstand?.let { enrich(it, fraOppslagsApi) }
                    )
                },
                arbeidssoekerId = hendelser.first().arbeidssoekerId,
                gjeldeneTilstand = sistePeriode?.let { enrich(it, fraOppslagsApi) }
            )
        }
    }

    private suspend fun hentFraOppslagsApi(identitetsnummer: String): Either<Error, List<ApiData>> {
        return either {
            val perioder = oppslagAPI.perioder(identitetsnummer).bind().map(ArbeidssoekerperiodeResponse::periodeId)
            val opplysninger =
                perioder.flatMap { periodeId ->
                    oppslagAPI.opplysninger(identitetsnummer, periodeId).bind()
                        .map { opplysning -> ApiData(periodeId, opplysning.opplysningerOmArbeidssoekerId, null) }
                        .let { it.ifEmpty { listOf(ApiData(periodeId, null, null)) } }
                }
            val profilernger = perioder.flatMap { periodeId ->
                oppslagAPI.profileringer(identitetsnummer, periodeId).bind()
                    .map { ApiData(periodeId, it.opplysningerOmArbeidssoekerId, it.profileringId) }
            }
            return perioder.map { periodeId -> ApiData(periodeId, null, null) }
                .filterNot { periode -> opplysninger.any { periode.periodeId == it.periodeId } }
                .plus(opplysninger.filterNot { opplysning -> profilernger.any { profilering -> profilering.periodeId == opplysning.periodeId } })
                .right()
                .onRight { result ->
                    apiOppslagLogger.info("Hentet data fra oppslagsapi, perioder: {}, opplysninger: {}, profileringer: {}",
                        result.distinctBy { it.periodeId }.size,
                        result.distinctBy { it.opplysningsId }.size,
                        result.distinctBy { it.profileringsId }.size)
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
    val profileringsId: UUID?
)

fun sistePeriode(hendelser: List<StoredData>): Tilstand? =
    hendelser
        .sortedBy { it.offset }
        .fold(null as Tilstand?, ::beregnTilstand)

fun historiskeTilstander(hendelser: List<StoredData>): Iterable<Snapshot> =
    hendelser.map(null as Tilstand?) { tilstand, hendelse ->
        val nyTilstand = beregnTilstand(tilstand, hendelse)
        val resultat = Snapshot(
            endret = nyTilstand !== tilstand,
            hendelse = Hendelse(
                hendelseId = hendelse.data.hendelseId,
                hendelseType = hendelse.data.hendelseType,
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
                kafkaOffset = hendelse.offset,
                data = hendelse.data,
                api = null
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


fun beregnTilstand(tilstand: Tilstand?, hendelse: StoredData): Tilstand? =
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

fun StoredData.erAvsluttet(): Boolean = this.data is Avsluttet
fun StoredData.erStartet(): Boolean = this.data is Startet
fun StoredData.erOpplysningerMottatt(): Boolean = this.data is OpplysningerOmArbeidssoekerMottatt

fun Tilstand?.harAktivPeriode(): Boolean {
    contract {
        returns(true) implies (this@harAktivPeriode != null)
    }
    return this != null && this.avsluttet == null
}
