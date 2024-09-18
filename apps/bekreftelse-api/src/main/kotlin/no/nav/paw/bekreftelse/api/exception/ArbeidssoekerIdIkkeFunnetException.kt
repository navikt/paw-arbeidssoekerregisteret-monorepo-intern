package no.nav.paw.bekreftelse.api.exception

import no.nav.paw.error.exception.AuthorizationException

class ArbeidssoekerIdIkkeFunnetException(message: String) :
    AuthorizationException("PAW_ARBEIDSSOEKER_ID_IKKE_FUNNET", message, null)