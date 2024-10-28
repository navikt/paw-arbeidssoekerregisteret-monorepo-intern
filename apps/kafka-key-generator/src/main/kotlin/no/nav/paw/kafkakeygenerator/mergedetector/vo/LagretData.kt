package no.nav.paw.kafkakeygenerator.mergedetector.vo

import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer

data class LagretData(
    val identitetsnummer: Identitetsnummer,
    val lagretData: Map<Identitetsnummer, ArbeidssoekerId?>
)