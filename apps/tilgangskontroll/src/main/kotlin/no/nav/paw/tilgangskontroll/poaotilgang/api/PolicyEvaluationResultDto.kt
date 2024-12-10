package no.nav.paw.tilgangskontroll.poaotilgang.api

import java.util.*

data class PolicyEvaluationResultDto (
    val requestId: UUID,
    val decision: DecisionDto
)