package no.nav.paw.kafkakeygenerator.mergedetector.vo

import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer

data class LagretData(
    val identitetsnummer: Identitetsnummer,
    val lagretData: Map<Identitetsnummer, ArbeidssoekerId?>
)