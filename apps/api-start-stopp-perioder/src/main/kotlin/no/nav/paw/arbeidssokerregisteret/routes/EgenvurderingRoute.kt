package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.paw.arbeidssoekerregisteret.api.egenvurdering.models.EgenvurderingRequest
import no.nav.paw.logging.logger.buildApplicationLogger

private val logger = buildApplicationLogger
const val egenvurderingPath = "/api/v1/arbeidssoker/profilering/egenvurdering"

fun Route.egenvurderingRoute() {
    post<EgenvurderingRequest>(egenvurderingPath) { egenvurderingRequest ->
        logger.info("Mottok egenvurderingrequest")
        call.respond(HttpStatusCode.Accepted)
    }
}