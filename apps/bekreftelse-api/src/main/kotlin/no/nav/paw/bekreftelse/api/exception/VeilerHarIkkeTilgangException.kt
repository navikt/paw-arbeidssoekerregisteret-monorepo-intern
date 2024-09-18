package no.nav.paw.bekreftelse.api.exception

import no.nav.paw.error.exception.AuthorizationException

class VeilerHarIkkeTilgangException(message: String) :
    AuthorizationException("PAW_VEILEDER_HAR_IKKE_TILGANG", message, null)