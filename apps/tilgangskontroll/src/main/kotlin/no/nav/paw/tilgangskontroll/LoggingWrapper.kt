package no.nav.paw.tilgangskontroll

import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.logging.logger.TeamLogsLogger
import no.nav.paw.tilgangskontroll.vo.Tilgang

fun TilgangsTjenesteForAnsatte.withSecureLogging(): TilgangsTjenesteForAnsatte {
    return LoggingWrapper( this)
}

class LoggingWrapper(
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
            TeamLogsLogger.trace(
                "Tilgangskontroll: ansatt={}, identitetsnummer={}, tilgangstype={}, har_tilgang={}",
                navIdent.value,
                identitetsnummer.value,
                tilgang,
                result ?: "error"
            )
        }
    }
}
