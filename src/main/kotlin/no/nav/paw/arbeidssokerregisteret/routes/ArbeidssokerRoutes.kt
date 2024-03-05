package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.paw.arbeidssokerregisteret.application.EndeligResultat
import no.nav.paw.arbeidssokerregisteret.application.RequestHandler
import no.nav.paw.arbeidssokerregisteret.application.TilgangskontrollResultat
import no.nav.paw.arbeidssokerregisteret.domain.http.KanStarteRequest
import no.nav.paw.arbeidssokerregisteret.domain.http.OpplysningerRequest
import no.nav.paw.arbeidssokerregisteret.domain.http.PeriodeTilstand
import no.nav.paw.arbeidssokerregisteret.domain.http.StartStoppRequest
import no.nav.paw.arbeidssokerregisteret.requestScope
import no.nav.paw.arbeidssokerregisteret.utils.logger

fun Route.arbeidssokerRoutes(requestHandler: RequestHandler) {
    route("/api/v1/arbeidssoker") {
        route("/kanStartePeriode") {
            put<KanStarteRequest> { request ->
                logger.trace("Sjekker om bruker kan registreres som arbeidssøker")
                val resultat = with(requestScope()) {
                    requestHandler.kanRegistreresSomArbeidssoker(request.getId())
                }
                logger.debug("Resultat av 'kan-starte': {}", resultat)
                respondWith(resultat)
            }
        }
        route("/periode") {
            put<StartStoppRequest> { startStoppRequest ->
                logger.trace("Registrerer bruker som arbeidssøker {}", startStoppRequest.periodeTilstand)
                val resultat = with(requestScope()) {
                    when (startStoppRequest.periodeTilstand) {
                        PeriodeTilstand.STARTET ->
                            requestHandler.startArbeidssokerperiode(startStoppRequest.getId())

                        PeriodeTilstand.STOPPET ->
                            requestHandler.avsluttArbeidssokerperiode(startStoppRequest.getId())
                    }
                }
                logger.debug("Registreringsresultat: {}", resultat)
                when (resultat) {
                    is TilgangskontrollResultat -> respondWith(resultat)
                    is EndeligResultat -> respondWith(resultat)
                }
            }
        }
        route("/opplysninger") {
            put {
                val opplysningerRequest = call.receive<OpplysningerRequest>()
                logger.trace("Registrerer eller oppdaterer brukers opplysninger")

                val resultat =
                    with(requestScope()) {
                        requestHandler.oppdaterBrukeropplysninger(opplysningerRequest)
                    }
                logger.debug("Oppdateringsresultat: {}", resultat)
                when (resultat) {
                    is TilgangskontrollResultat -> respondWith(resultat)
                    is EndeligResultat -> respondWith(resultat)
                }
            }
        }
    }
}
