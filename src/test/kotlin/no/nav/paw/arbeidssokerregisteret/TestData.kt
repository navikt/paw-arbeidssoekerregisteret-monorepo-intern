package no.nav.paw.arbeidssokerregisteret

import no.nav.paw.arbeidssokerregisteret.domain.Foedselsnummer
import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import java.util.*

object TestData {
    val foedselsnummer = Foedselsnummer("18908396568")
    val navAnsatt = NavAnsatt(UUID.randomUUID(), "Z999999")
}
