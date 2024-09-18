package no.nav.paw.bekreftelse.api.exception

import no.nav.paw.error.exception.AuthorizationException

class UkjentBearerTokenException(message: String) :
    AuthorizationException("PAW_UKJENT_BEARER_TOKEN", message, null)