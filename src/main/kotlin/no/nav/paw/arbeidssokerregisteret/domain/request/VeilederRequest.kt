package no.nav.paw.arbeidssokerregisteret.domain.request

import no.nav.paw.arbeidssokerregisteret.domain.Foedselsnummer

data class VeilederRequest(val foedselsnummer: String) {
    fun getFoedselsnummer(): Foedselsnummer = Foedselsnummer(foedselsnummer)
}
