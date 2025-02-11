package no.nav.paw.security.authorization.exception

import no.nav.paw.error.model.asSecurityErrorType

class IngenTilgangException(message: String) :
    AuthorizationException("ingen-tilgang".asSecurityErrorType(), message)