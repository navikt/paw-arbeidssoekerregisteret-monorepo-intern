package no.nav.paw.security.authentication.exception

import no.nav.paw.error.model.asSecurityErrorType

class BearerTokenManglerException(message: String) :
    AuthenticationException("bearer-token-mangler".asSecurityErrorType(), message)