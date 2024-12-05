package no.nav.paw.security.authorization.exception

import no.nav.paw.error.model.ErrorType

class UgyldigBearerTokenException(message: String) :
    AuthorizationException(ErrorType.domain("security").error("ugyldig-bearer-token").build(), message)