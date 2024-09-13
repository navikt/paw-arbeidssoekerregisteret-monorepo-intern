package no.nav.paw.bekreftelse.api.model

import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig

data class InternState(
    val tilgjendeligeBekreftelser: List<BekreftelseTilgjengelig>
)
