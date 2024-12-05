package no.nav.paw.tilgangskontroll.poaotilgang.api

import java.util.UUID

interface PolicyInput

data class PolicyEvaluationRequestDto<PI: PolicyInput>(
    val requestId: UUID,
    val policyInput: PI,
    val policyId: PolicyId
)
