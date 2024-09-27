package no.nav.paw.bekreftelse.api.exception

import no.nav.paw.error.exception.AuthorizationException

class UgyldigBearerTokenException(message: String) :
    AuthorizationException("PAW_UGYLDIG_BEARER_TOKEN", message, null)