package no.nav.paw.tilgangskontroll

import no.nav.paw.tilgangskontroll.vo.Identitetsnummer
import no.nav.paw.tilgangskontroll.vo.NavIdent
import no.nav.paw.tilgangskontroll.vo.Tilgang
import org.slf4j.Logger
import org.slf4j.Marker

fun TilgangsTjenesteForAnsatte.withSecureLogging(secureLogger: SecureLogger): TilgangsTjenesteForAnsatte {
    return LoggingWrapper(secureLogger, this)
}

class LoggingWrapper(
    private val secureLogger: SecureLogger,
    private val backend: TilgangsTjenesteForAnsatte
): TilgangsTjenesteForAnsatte {
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
