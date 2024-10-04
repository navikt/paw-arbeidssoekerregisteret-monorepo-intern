package no.nav.paw.bekreftelseutgang.tilstand

import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

typealias StateStore = KeyValueStore<UUID, InternTilstand>

@JvmRecord
data class InternTilstand(
    val identitetsnummer: String?,
    val bekreftelseHendelse: BekreftelseHendelse?,
)
