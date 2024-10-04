package no.nav.paw.bekreftelseutgang.tilstand

import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelseutgang.config.ApplicationConfig
import no.nav.paw.bekreftelseutgang.topology.processBekreftelseHendelse
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

typealias StateStore = KeyValueStore<UUID, InternTilstand>

@JvmRecord
data class InternTilstand(
    val identitetsnummer: String?,
    val bekreftelseHendelse: BekreftelseHendelse?,
)

fun InternTilstand.generateAvsluttetEventIfStateIsComplete(applicationConfig: ApplicationConfig): Avsluttet? =
    if(identitetsnummer != null && bekreftelseHendelse != null) {
        processBekreftelseHendelse(
            bekreftelseHendelse = bekreftelseHendelse,
            identitetsnummer = identitetsnummer,
            applicationConfig = applicationConfig
        )
    } else {
        null
    }

