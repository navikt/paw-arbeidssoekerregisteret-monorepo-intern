package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.paw.arbeidssokerregisteret.application.RequestHandler
import no.nav.paw.arbeidssokerregisteret.domain.http.Request
import no.nav.paw.arbeidssokerregisteret.requestScope
import no.nav.paw.arbeidssokerregisteret.utils.logger

fun Route.arbeidssokerRoutes(requestHandler: RequestHandler) {
    route("/api/v1") {
        authenticate("tokenx", "azure") {
            route("/arbeidssoker/perioder") {
                route("/kan-starte") {
                    put {
                        logger.trace("Sjekker om bruker kan registreres som arbeidssøker")
                        val request = call.receive<Request>()
                        val resultat = with(requestScope()) {
                            requestHandler.kanRegistreresSomArbeidssoker(request.getIdentitetsnummer())
                        }
                        logger.debug("Resultat av 'kan-starte': {}", resultat)
                        respondWith(resultat)
                    }
                }
                route("/") {
                    put {
                        logger.trace("Registrerer bruker som arbeidssøker")
                        val request = call.receive<Request>()
                        val resultat = with(requestScope()) {
                            requestHandler.startArbeidssokerperiode(request.getIdentitetsnummer())
                        }
                        logger.debug("Registreringsresultat: {}", resultat)
                        respondWith(resultat)
                    }
                }
                route("/") {
                    delete {
                        logger.trace("Avslutter periode")
                        val request = call.receive<Request>()
                        val resultat = with(requestScope()) {
                            requestHandler.avsluttArbeidssokerperiode(request.getIdentitetsnummer())
                        }
                        logger.debug("Registreringsresultat: {}", resultat)
                        respondWith(resultat)
                    }
                }
            }
        }
    }
}
