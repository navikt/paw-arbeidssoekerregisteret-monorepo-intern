package no.nav.paw.kafkakeygenerator.api.v2

import no.nav.paw.kafkakeygenerator.model.Info
import no.nav.paw.kafkakeygenerator.model.MergeDetected

data class InfoResponse(
    val info: Info,
    val mergeDetected: MergeDetected?
)