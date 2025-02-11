package no.nav.paw.security.authorization.exception

import no.nav.paw.error.model.asSecurityErrorType

class UgyldigBrukerException(message: String) :
    AuthorizationException("ugyldig-bruker".asSecurityErrorType(), message)