package no.nav.paw.security.authorization.model

enum class Decision {
    PERMIT,
    DENY,
    DEFER
}

sealed class AccessDecision(
    val decision: Decision,
    open val description: String
)

data class Permit(override val description: String) : AccessDecision(Decision.PERMIT, description)
data class Deny(override val description: String) : AccessDecision(Decision.DENY, description)
data class Defer(override val description: String) : AccessDecision(Decision.DEFER, description)
