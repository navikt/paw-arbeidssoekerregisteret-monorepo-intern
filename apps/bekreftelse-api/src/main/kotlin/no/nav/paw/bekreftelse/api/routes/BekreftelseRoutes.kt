package no.nav.paw.bekreftelse.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.context.resolveRequest
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.poao_tilgang.client.TilgangType

fun Route.bekreftelseRoutes(applicationContext: ApplicationContext) {
    val authorizationService = applicationContext.authorizationService
    val bekreftelseService = applicationContext.bekreftelseService

    route("/api/v1") {
        authenticate("idporten", "tokenx", "azure") {
            get("/tilgjengelige-bekreftelser") {
                val requestContext = resolveRequest()
                val securityContext = authorizationService.authorize(requestContext, TilgangType.LESE)
                val response = bekreftelseService.finnTilgjengeligBekreftelser(
                    securityContext,
                    TilgjengeligeBekreftelserRequest(securityContext.sluttbruker.identitetsnummer),
                    requestContext.useMockData
                )
                call.respond(HttpStatusCode.OK, response)
            }

            post<TilgjengeligeBekreftelserRequest>("/tilgjengelige-bekreftelser") { request ->
                val requestContext = resolveRequest(request.identitetsnummer)
                val securityContext = authorizationService.authorize(requestContext, TilgangType.LESE)
                val response = bekreftelseService.finnTilgjengeligBekreftelser(
                    securityContext,
                    request,
                    requestContext.useMockData
                )
                call.respond(HttpStatusCode.OK, response)
            }

            post<BekreftelseRequest>("/bekreftelse") { request ->
                val requestContext = resolveRequest(request.identitetsnummer)
                val securityContext = authorizationService.authorize(requestContext, TilgangType.SKRIVE)
                bekreftelseService.mottaBekreftelse(
                    securityContext,
                    request,
                    requestContext.useMockData
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
