package no.nav.paw.security.authorization.context

data class AuthorizationContext(
    val requestContext: RequestContext,
    val securityContext: SecurityContext
)
