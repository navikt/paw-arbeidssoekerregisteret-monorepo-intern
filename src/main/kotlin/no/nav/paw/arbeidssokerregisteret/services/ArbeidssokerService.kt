package no.nav.paw.arbeidssokerregisteret.services

import no.nav.paw.arbeidssokerregisteret.domain.Foedselsnummer
import no.nav.paw.pdl.PdlClient
import java.time.LocalDateTime

class ArbeidssokerService(pdlClient: PdlClient) {
    fun startArbeidssokerperiode(foedselsnummer: Foedselsnummer, fraOgMedDato: LocalDateTime = LocalDateTime.now()) {
        TODO()
    }

    fun avsluttArbeidssokerperiode(foedselsnummer: Foedselsnummer, tilOgMedDato: LocalDateTime = LocalDateTime.now()) {
        TODO()
    }
}
