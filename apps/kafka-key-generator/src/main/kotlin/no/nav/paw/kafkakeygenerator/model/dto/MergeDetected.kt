package no.nav.paw.kafkakeygenerator.model.dto

import no.nav.paw.felles.model.ArbeidssoekerId
import no.nav.paw.felles.model.Identitetsnummer

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