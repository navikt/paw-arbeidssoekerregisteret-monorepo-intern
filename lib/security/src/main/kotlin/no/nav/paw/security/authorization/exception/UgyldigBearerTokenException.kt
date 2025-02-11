package no.nav.paw.security.authorization.exception

import no.nav.paw.error.model.asSecurityErrorType

class UgyldigBearerTokenException(message: String) :
    AuthorizationException("ugyldig-bearer-token".asSecurityErrorType(), message)