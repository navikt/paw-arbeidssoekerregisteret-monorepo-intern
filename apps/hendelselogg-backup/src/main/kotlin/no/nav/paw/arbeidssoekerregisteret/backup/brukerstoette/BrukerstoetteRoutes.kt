package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.DetaljerRequest
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.bruker
import org.slf4j.LoggerFactory

private val auditLogger = LoggerFactory.getLogger("audit_logger")

fun Route.brukerstoetteRoutes(
    brukerstoetteService: BrukerstoetteService,
) {
    post("/arbeidssoeker/detaljer") {
        if (currentRuntimeEnvironment is ProdGcp) {
            val bruker = call.bruker<NavAnsatt>()
            auditLogger.info("Brukerstoette request fra navIdent='${bruker.ident}' med oid='${bruker.oid}'")
        }
        val request: DetaljerRequest = call.receive()
        val detaljer = brukerstoetteService.hentDetaljer(request.identitetsnummer)
        call.respond(detaljer)
    }
}

fun Route.apiDocsRoutes(
    path: String = "docs/brukerstoette",
    swaggerFile: String = "openapi/Brukerstoette.yaml",
) {
    swaggerUI(path, swaggerFile)
}



