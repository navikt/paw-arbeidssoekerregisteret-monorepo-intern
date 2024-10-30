package no.nav.paw.security.authorization.model

data class AccessResult(
    val decision: AccessDecision,
    val details: String
)