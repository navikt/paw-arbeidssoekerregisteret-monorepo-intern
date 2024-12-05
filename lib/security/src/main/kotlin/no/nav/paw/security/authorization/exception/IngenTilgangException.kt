package no.nav.paw.security.authorization.exception

import no.nav.paw.error.model.ErrorType

class IngenTilgangException(message: String) :
    AuthorizationException(ErrorType.domain("security").error("ingen-tilgang").build(), message)