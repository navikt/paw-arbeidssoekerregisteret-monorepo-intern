package no.nav.paw.arbeidssokerregisteret.domain.request

import no.nav.paw.arbeidssokerregisteret.domain.Foedselsnummer
import no.nav.paw.arbeidssokerregisteret.domain.toFoedselsnummer

data class VeilederRequest(val foedselsnummer: String) {
    fun getFoedselsnummer(): Foedselsnummer = foedselsnummer.toFoedselsnummer()
}
