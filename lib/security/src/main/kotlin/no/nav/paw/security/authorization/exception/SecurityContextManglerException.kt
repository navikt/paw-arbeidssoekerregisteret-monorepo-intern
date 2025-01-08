package no.nav.paw.security.authorization.exception

import no.nav.paw.error.model.ErrorType

class SecurityContextManglerException(message: String) :
    AuthorizationException(ErrorType.domain("security").error("security-context-mangler").build(), message)