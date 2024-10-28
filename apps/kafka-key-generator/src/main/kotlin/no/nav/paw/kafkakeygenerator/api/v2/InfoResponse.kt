package no.nav.paw.kafkakeygenerator.api.v2

import no.nav.paw.kafkakeygenerator.mergedetector.vo.MergeDetected
import no.nav.paw.kafkakeygenerator.vo.Info

data class InfoResponse(
    val info: Info,
    val mergeDetected: MergeDetected?
)