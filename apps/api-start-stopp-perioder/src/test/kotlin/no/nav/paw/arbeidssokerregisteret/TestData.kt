package no.nav.paw.arbeidssokerregisteret

import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import no.nav.paw.felles.model.asIdentitetsnummer
import java.util.*

object TestData {
    val foedselsnummer = "18908396568".asIdentitetsnummer()
    val navAnsatt = NavAnsatt(UUID.randomUUID(), "Z999999")
}
