package no.nav.paw.arbeidssokerregisteret.domain.http

import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.tilIdentitetsnummer

data class Request(
    val identitetsnummer: String,
    val registreringForhaandsGodkjentAvAnsatt: Boolean = false
) {
    fun getIdentitetsnummer(): Identitetsnummer = identitetsnummer.tilIdentitetsnummer()
}
