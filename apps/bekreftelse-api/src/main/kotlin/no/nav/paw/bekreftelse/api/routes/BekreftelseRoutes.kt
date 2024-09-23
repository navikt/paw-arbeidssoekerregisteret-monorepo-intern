package no.nav.paw.bekreftelse.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.bekreftelse.api.authz.authorize
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.poao_tilgang.client.TilgangType

fun Route.bekreftelseRoutes(applicationContext: ApplicationContext) {
    val authorizationService = applicationContext.authorizationService
    val bekreftelseService = applicationContext.bekreftelseService

    route("/api/v1") {
        authenticate("idporten", "tokenx", "azure") {
            get("/tilgjengelige-bekreftelser") {
                with(authorize(null, authorizationService, TilgangType.LESE)) {
                    val tilgjengeligeBekreftelser = bekreftelseService
                        .finnTilgjengeligBekreftelser(
                            sluttbruker,
                            innloggetBruker,
                            TilgjengeligeBekreftelserRequest(sluttbruker.identitetsnummer),
                            useMockData
                        )

                    call.respond(HttpStatusCode.OK, tilgjengeligeBekreftelser)

                    // TODO Exception handling
                }
            }
            post<TilgjengeligeBekreftelserRequest>("/tilgjengelige-bekreftelser") { request ->
                with(authorize(request.identitetsnummer, authorizationService, TilgangType.LESE)) {
                    val tilgjengeligeBekreftelser = bekreftelseService
                        .finnTilgjengeligBekreftelser(
                            sluttbruker,
                            innloggetBruker,
                            request,
                            useMockData
                        )

                    call.respond(HttpStatusCode.OK, tilgjengeligeBekreftelser)

                    // TODO Exception handling
                }

            }
            post<BekreftelseRequest>("/bekreftelse") { request ->
                with(authorize(request.identitetsnummer, authorizationService, TilgangType.SKRIVE)) {
                    bekreftelseService.mottaBekreftelse(
                        sluttbruker,
                        innloggetBruker,
                        request,
                        useMockData
                    )

                    call.respond(HttpStatusCode.OK)

                    // TODO Exception handling
                }
            }
        }
    }
}

