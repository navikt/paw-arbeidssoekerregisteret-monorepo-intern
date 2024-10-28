package no.nav.paw.kafkakeygenerator.mergedetector

import no.nav.paw.kafkakeygenerator.mergedetector.vo.LagretData
import no.nav.paw.kafkakeygenerator.mergedetector.vo.MergeDetected
import no.nav.paw.kafkakeygenerator.mergedetector.vo.MergeDetectorResult
import no.nav.paw.kafkakeygenerator.mergedetector.vo.NoMergeDetected

fun findMerge(lagretData: LagretData): MergeDetectorResult {
    val etterArbeidssoekerId = lagretData.lagretData
        .toList()
        .mapNotNull { (id, arbeidssoekerId) -> arbeidssoekerId?.let { it to id } }
        .groupBy { it.first }
        .toMap()
        .mapValues { (_, value) -> value.map { it.second } }
    return if (etterArbeidssoekerId.size > 1) {
        MergeDetected(lagretData.identitetsnummer, etterArbeidssoekerId)
    } else {
        NoMergeDetected(lagretData.identitetsnummer)
    }
}