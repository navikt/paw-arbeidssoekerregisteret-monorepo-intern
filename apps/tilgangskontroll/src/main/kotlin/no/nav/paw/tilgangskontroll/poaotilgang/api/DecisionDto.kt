package no.nav.paw.tilgangskontroll.poaotilgang.api

data class DecisionDto(
    val type: DecisionType,
    val message: String?,
    val reason: String?
)

enum class DecisionType {
    PERMIT, DENY
}
