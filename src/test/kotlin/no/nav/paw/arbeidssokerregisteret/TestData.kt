package no.nav.paw.arbeidssokerregisteret

import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import no.nav.paw.arbeidssokerregisteret.domain.toFoedselsnummer
import java.util.*

object TestData {
    val foedselsnummer = "18908396568".toFoedselsnummer()
    val navAnsatt = NavAnsatt(UUID.randomUUID(), "Z999999")
}
