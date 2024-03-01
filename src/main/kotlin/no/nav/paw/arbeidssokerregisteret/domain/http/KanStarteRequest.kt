package no.nav.paw.arbeidssokerregisteret.domain.http

import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.tilIdentitetsnummer

data class KanStarteRequest(
    val identitetsnummer: String
) {
    fun getId(): Identitetsnummer = identitetsnummer.tilIdentitetsnummer()
}
