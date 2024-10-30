package no.nav.paw.security.authentication.exception

import no.nav.paw.error.exception.AuthorizationException

class UgyldigBrukerException(message: String) :
    AuthorizationException("PAW_UGYLDIG_BRUKER", message)