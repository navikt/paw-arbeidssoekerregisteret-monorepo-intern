package no.nav.paw.meldeplikttjeneste

import no.nav.paw.meldeplikttjeneste.tilstand.InternTilstandSerde
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelseSerde

class ApplicationContext(
    val internTilstandSerde: InternTilstandSerde,
    val rapporteringsHendelseSerde: RapporteringsHendelseSerde
)