package no.nav.paw.security.authorization.exception

import no.nav.paw.error.model.ErrorType

class UgyldigBrukerException(message: String) :
    AuthorizationException(ErrorType.domain("security").error("ugyldig-bruker").build(), message)