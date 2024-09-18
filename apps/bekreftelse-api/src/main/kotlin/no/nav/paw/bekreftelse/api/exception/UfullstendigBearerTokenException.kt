package no.nav.paw.bekreftelse.api.exception

import no.nav.paw.error.exception.AuthorizationException

class UfullstendigBearerTokenException(message: String) :
    AuthorizationException("PAW_UFULLSTENDIG_BEARER_TOKEN", message, null)