package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1

data class FunctionContext<S: TilstandV1?, K>(
    val tilstand: S,
    val scope: HendelseScope<K>
)
