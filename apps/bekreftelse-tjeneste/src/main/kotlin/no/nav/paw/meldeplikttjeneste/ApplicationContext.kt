package no.nav.paw.meldeplikttjeneste

import no.nav.paw.meldeplikttjeneste.tilstand.InternTilstandSerde
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde

class ApplicationContext(
    val internTilstandSerde: InternTilstandSerde,
    val bekreftelseHendelseSerde: BekreftelseHendelseSerde
)