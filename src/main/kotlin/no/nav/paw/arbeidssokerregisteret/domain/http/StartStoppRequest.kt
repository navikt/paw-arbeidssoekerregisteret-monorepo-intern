package no.nav.paw.arbeidssokerregisteret.domain.http

import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.tilIdentitetsnummer

data class StartStoppRequest(
    val identitetsnummer: String,
    val registreringForhaandsGodkjentAvAnsatt: Boolean = false,
    val periodeTilstand: PeriodeTilstand
) {
    fun getId(): Identitetsnummer = identitetsnummer.tilIdentitetsnummer()
}

enum class PeriodeTilstand{
    STARTET,
    STOPPET
}
