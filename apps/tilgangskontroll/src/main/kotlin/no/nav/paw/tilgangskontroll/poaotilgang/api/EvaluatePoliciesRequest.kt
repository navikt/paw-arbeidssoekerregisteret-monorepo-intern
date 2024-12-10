package no.nav.paw.tilgangskontroll.poaotilgang.api

data class EvaluatePoliciesRequest<PI: PolicyInput> (
    val requests: List<PolicyEvaluationRequestDto<out PI>>
)

fun Iterable<PolicyEvaluationRequestDto<out PolicyInput>>.toEvaluatePoliciesRequest(): EvaluatePoliciesRequest<out PolicyInput> {
    return EvaluatePoliciesRequest(toList())
}