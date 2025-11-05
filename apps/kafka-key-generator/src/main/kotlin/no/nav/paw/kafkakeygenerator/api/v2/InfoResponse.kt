package no.nav.paw.kafkakeygenerator.api.v2

import no.nav.paw.kafkakeygenerator.model.dto.Info
import no.nav.paw.kafkakeygenerator.model.dto.MergeDetected

data class InfoResponse(
    val info: Info,
    val mergeDetected: MergeDetected?
)