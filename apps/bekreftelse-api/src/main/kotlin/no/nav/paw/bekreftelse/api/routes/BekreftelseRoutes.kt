package no.nav.paw.bekreftelse.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.services.AutorisasjonService
import no.nav.paw.bekreftelse.api.services.BekreftelseService
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.poao_tilgang.client.TilgangType

fun Route.bekreftelseRoutes(
    kafkaKeyClient: KafkaKeysClient,
    bekreftelseService: BekreftelseService,
    autorisasjonService: AutorisasjonService
) {
    route("/api/v1") {
        authenticate("tokenx", "azure") {
            post<TilgjengeligeBekreftelserRequest>("/tilgjengelige-bekreftelser") { request ->
                with(requestScope(request.identitetsnummer, autorisasjonService, kafkaKeyClient, TilgangType.LESE)) {
                    val arbeidssoekerId = this

                    val bearerToken = requireNotNull(call.request.headers["Authorization"]) {
                        "Authorization header is missing"
                    }

                    val tilgjengeligeBekreftelser = bekreftelseService
                        .finnTilgjengeligBekreftelser(bearerToken, arbeidssoekerId, request)

                    call.respond(HttpStatusCode.OK, tilgjengeligeBekreftelser)

                    // TODO Exception handling
                }

            }
            post<BekreftelseRequest>("/bekreftelse") { request ->
                with(requestScope(request.identitetsnummer, autorisasjonService, kafkaKeyClient, TilgangType.SKRIVE)) {
                    val arbeidssoekerId = this

                    val bearerToken = requireNotNull(call.request.headers["Authorization"]) {
                        "Authorization header is missing"
                    }

                    bekreftelseService.mottaBekreftelse(bearerToken, arbeidssoekerId, request)

                    call.respond(HttpStatusCode.OK)

                    // TODO Exception handling
                }
            }
        }
    }
}

