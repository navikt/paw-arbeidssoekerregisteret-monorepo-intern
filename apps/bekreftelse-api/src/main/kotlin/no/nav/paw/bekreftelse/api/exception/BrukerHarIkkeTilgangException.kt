package no.nav.paw.bekreftelse.api.exception

import no.nav.paw.error.exception.AuthorizationException

class BrukerHarIkkeTilgangException(message: String) :
    AuthorizationException("PAW_BRUKER_HAR_IKKE_TILGANG", message)