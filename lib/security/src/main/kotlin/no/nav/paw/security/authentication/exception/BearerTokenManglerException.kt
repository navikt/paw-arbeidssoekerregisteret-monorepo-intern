package no.nav.paw.security.authentication.exception

import no.nav.paw.error.model.ErrorType

class BearerTokenManglerException(message: String) :
    AuthenticationException(ErrorType.domain("security").error("bearer-token-mangler").build(), message)