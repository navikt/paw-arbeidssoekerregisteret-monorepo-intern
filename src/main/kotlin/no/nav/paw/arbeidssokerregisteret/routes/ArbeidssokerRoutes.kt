package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.paw.arbeidssokerregisteret.domain.request.VeilederRequest
import no.nav.paw.arbeidssokerregisteret.services.ArbeidssokerService
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.utils.getNavAnsatt
import no.nav.paw.arbeidssokerregisteret.utils.getPidClaim
import no.nav.paw.arbeidssokerregisteret.utils.logger

fun Route.arbeidssokerRoutes(arbeidssokerService: ArbeidssokerService, autorisasjonService: AutorisasjonService) {
    route("/api/v1") {
        authenticate("tokenx") {
            route("/arbeidssoker/perioder") {
                post {
                    logger.info("Starter ny arbeidssøkerperiode for bruker")

                    val foedselsnummer = call.getPidClaim()

                    arbeidssokerService.startArbeidssokerperiode(foedselsnummer, foedselsnummer.verdi)

                    logger.info("Startet arbeidssøkerperiode for bruker")

                    call.respond(HttpStatusCode.Accepted)
                }

                put {
                    logger.info("Avslutter arbeidssøkerperiode for bruker")

                    val foedselsnummer = call.getPidClaim()
                    arbeidssokerService.avsluttArbeidssokerperiode(foedselsnummer, foedselsnummer.verdi)

                    logger.info("Avsluttet arbeidssøkerperiode for bruker")

                    call.respond(HttpStatusCode.Accepted)
                }
            }
        }

        route("/veileder/arbeidssoker/perioder") {
            authenticate("azure") {
                post {
                    logger.info("Veileder starter ny arbeidssøkerperiode for bruker")

                    val foedselsnummer = call.receive<VeilederRequest>().getFoedselsnummer()
                    val navAnsatt = call.getNavAnsatt()

                    val harNavBrukerTilgang =
                        autorisasjonService.verifiserVeilederTilgangTilBruker(navAnsatt, foedselsnummer)

                    if (!harNavBrukerTilgang) {
                        return@post call.respond(HttpStatusCode.Forbidden, "NAV-ansatt har ikke tilgang")
                    }

                    arbeidssokerService.startArbeidssokerperiode(foedselsnummer, navAnsatt.ident)

                    logger.info("Veileder startet arbeidssøkerperiode for bruker")

                    call.respond(HttpStatusCode.Accepted)
                }
            }
        }
    }
}
