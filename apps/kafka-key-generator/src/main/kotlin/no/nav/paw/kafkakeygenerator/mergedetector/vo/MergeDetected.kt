package no.nav.paw.kafkakeygenerator.mergedetector.vo

import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer

interface MergeDetectorResult {
    val id: Identitetsnummer
}

data class NoMergeDetected(
    override val id: Identitetsnummer
) : MergeDetectorResult

data class MergeDetected(
    override val id: Identitetsnummer,
    val map: Map<ArbeidssoekerId, List<Identitetsnummer>>
) : MergeDetectorResult