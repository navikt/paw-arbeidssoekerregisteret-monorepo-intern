package no.nav.paw.bekreftelse.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.bekreftelse.api.authz.requestScope
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.services.AutorisasjonService
import no.nav.paw.bekreftelse.api.services.BekreftelseService
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.poao_tilgang.client.TilgangType

fun Route.bekreftelseRoutes(
    kafkaKeyClient: KafkaKeysClient,
    autorisasjonService: AutorisasjonService,
    bekreftelseService: BekreftelseService
) {
    route("/api/v1") {
        authenticate("tokenx", "azure") {
            get("/tilgjengelige-bekreftelser") {
                with(requestScope(null, kafkaKeyClient, autorisasjonService, TilgangType.LESE)) {
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
                with(requestScope(request.identitetsnummer, kafkaKeyClient, autorisasjonService, TilgangType.LESE)) {
                    val tilgjengeligeBekreftelser = bekreftelseService
                        .finnTilgjengeligBekreftelser(sluttbruker, innloggetBruker, request, useMockData)

                    call.respond(HttpStatusCode.OK, tilgjengeligeBekreftelser)

                    // TODO Exception handling
                }

            }
            post<BekreftelseRequest>("/bekreftelse") { request ->
                with(requestScope(request.identitetsnummer, kafkaKeyClient, autorisasjonService, TilgangType.SKRIVE)) {
                    bekreftelseService.mottaBekreftelse(sluttbruker, innloggetBruker, request, useMockData)

                    call.respond(HttpStatusCode.OK)

                    // TODO Exception handling
                }
            }
        }
    }
}

