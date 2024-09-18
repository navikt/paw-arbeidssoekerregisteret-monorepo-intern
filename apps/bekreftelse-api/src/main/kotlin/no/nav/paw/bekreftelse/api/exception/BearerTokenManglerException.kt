package no.nav.paw.bekreftelse.api.exception

import no.nav.paw.error.exception.AuthenticationException

class BearerTokenManglerException(message: String) : AuthenticationException("PAW_BEARER_TOKEN_MANGLER", message, null)