package no.nav.paw.security.authorization.exception

import no.nav.paw.error.exception.AuthorizationException

class IngenTilgangException(message: String) :
    AuthorizationException("PAW_INGEN_TILGANG", message)