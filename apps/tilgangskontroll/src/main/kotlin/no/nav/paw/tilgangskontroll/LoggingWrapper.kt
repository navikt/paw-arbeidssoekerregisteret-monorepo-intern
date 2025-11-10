package no.nav.paw.tilgangskontroll

import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.tilgangskontroll.vo.Tilgang

fun TilgangsTjenesteForAnsatte.withSecureLogging(secureLogger: SecureLogger): TilgangsTjenesteForAnsatte {
    return LoggingWrapper(secureLogger, this)
}

class LoggingWrapper(
    private val secureLogger: SecureLogger,
    private val backend: TilgangsTjenesteForAnsatte
) : TilgangsTjenesteForAnsatte {
    override suspend fun harAnsattTilgangTilPerson(
        navIdent: NavIdent,
        identitetsnummer: Identitetsnummer,
        tilgang: Tilgang
    ): Boolean {
        var result: Boolean? = null
        try {
            result = backend.harAnsattTilgangTilPerson(navIdent, identitetsnummer, tilgang)
            return result
        } finally {
            secureLogger.trace(
                "Tilgangskontroll: ansatt={}, identitetsnummer={}, tilgangstype={}, har_tilgang={}",
                navIdent.value,
                identitetsnummer.value,
                tilgang,
                result ?: "error"
            )
        }
    }
}
