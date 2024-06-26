@file:OptIn(ExperimentalContracts::class)

package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.*
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Hendelse
import no.nav.paw.arbeidssoekerregisteret.backup.database.getOneRecordForId
import no.nav.paw.arbeidssoekerregisteret.backup.database.readAllRecordsForId
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.vo.StoredData
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class BrukerstoetteService(
    private val oppslagAPI: OppslagApiClient,
    private val kafkaKeysClient: KafkaKeysClient,
    private val applicationContext: ApplicationContext,
    private val hendelseDeserializer: HendelseDeserializer
) {

    suspend fun hentDetaljer(identitetsnummer: String): DetaljerResponse? {
        val hendelser = transaction {
            (::getOneRecordForId)(hendelseDeserializer, applicationContext, identitetsnummer)
                ?.arbeidssoekerId
                ?.let{ id -> (::readAllRecordsForId)(hendelseDeserializer, applicationContext, id) }
                ?: emptyList()
        }
        if (hendelser.isEmpty()) {
            return null
        } else {
            val sistePeriode = sistePeriode(hendelser)
            val innkommendeHendelse = historiskeTilstander(hendelser).toList()
            val partition = hendelser.firstOrNull()?.partition
            return DetaljerResponse(
                recordKey = hendelser.first().recordKey,
                kafkaPartition = partition,
                historikk = innkommendeHendelse,
                arbeidssoekerId = hendelser.first().arbeidssoekerId,
                gjeldeneTilstand = sistePeriode
            )
        }
    }
}

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
                apiKall = null
            )
        }

        !tilstand.harAktivPeriode() -> tilstand
        tilstand.harAktivPeriode() && hendelse.erOpplysningerMottatt() -> {
            tilstand.copy(
                harOpplysningerMottattHendelse = true
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
