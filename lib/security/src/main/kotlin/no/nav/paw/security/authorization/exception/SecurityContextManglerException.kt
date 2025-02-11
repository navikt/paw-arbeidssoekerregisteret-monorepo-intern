package no.nav.paw.security.authorization.exception

import no.nav.paw.error.model.asSecurityErrorType

class SecurityContextManglerException(message: String) :
    AuthorizationException("security-context-mangler".asSecurityErrorType(), message)