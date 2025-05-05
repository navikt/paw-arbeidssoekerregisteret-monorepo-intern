package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.DetaljerRequest
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Feil
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authentication.plugin.autentisering
import org.slf4j.LoggerFactory

private val auditLogger = LoggerFactory.getLogger("audit_logger")
private val errorLogger = LoggerFactory.getLogger("error_logger")

fun Route.bekreftelseRoutes(
    brukerstoetteService: BrukerstoetteService
) {
    route("/api/v1") {
        autentisering(AzureAd) {
            post("/arbeidssoeker/detaljer") {
                runCatching {
                    val bruker = call.bruker<NavAnsatt>()
                    auditLogger.info("Brukerstoette request fra navIdent='${bruker.ident}' med oid='${bruker.oid}'")
                    val request: DetaljerRequest = call.receive()
                    brukerstoetteService.hentDetaljer(request.identitetsnummer)
                }.onSuccess { detaljer ->
                    detaljer?.let { call.respond(it) } ?: call.respond(
                        HttpStatusCode.NotFound, Feil(
                            melding = "Ingen hendelser for bruker",
                            feilKode = "ikke funnet"
                        )
                    )
                }.onFailure {
                    errorLogger.error("Feil ved henting av detaljer", it)
                    call.respond(
                        HttpStatusCode.InternalServerError, Feil(
                            melding = "Feil ved henting av detaljer",
                            feilKode = "intern feil"
                        )
                    )
                }
            }
        }
    }
}



fun Route.apiDocsRoutes(
    path: String = "docs",
    swaggerFile: String = "openapi/documentation.yaml"
) {
    swaggerUI(path, swaggerFile)
}



