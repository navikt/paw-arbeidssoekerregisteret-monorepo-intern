package no.nav.paw.arbeidssokerregisteret.arbeidssoker

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.paw.arbeidssokerregisteret.utils.getPidClaim
import no.nav.paw.arbeidssokerregisteret.utils.logger

fun Route.arbeidssokerRoutes(arbeidssokerService: ArbeidssokerService) {
    route("/arbeidssoker/") {
        authenticate("tokenx") {
            route("/perioder") {
                get {
                    logger.info("Henter arbeidssøkerperioder for bruker")

                    val foedselsnummer = call.getPidClaim()
                    val perioder = arbeidssokerService.hentPerioder(foedselsnummer)

                    perioder.ifEmpty {
                        return@get call.respond(HttpStatusCode.NotFound, "Arbeidssøker ikke funnet")
                    }

                    logger.info("Hentet arbeidssøkerperioder for bruker")
                    call.respond(HttpStatusCode.OK, perioder)
                }

                post {
                    logger.info("Starter ny arbeidssøkerperiode for bruker")

                    val foedselsnummer = call.getPidClaim()
                    arbeidssokerService.startPeriode(foedselsnummer)

                    logger.info("Startet arbeidssøkerperiode for bruker")
                    call.respond(HttpStatusCode.OK, "Opprett arbeidssøkerperiode")
                }

                put {
                    logger.info("Avslutter arbeidssøkerperiode for bruker")

                    val foedselsnummer = call.getPidClaim()
                    arbeidssokerService.avsluttPeriode(foedselsnummer)

                    logger.info("Avsluttet arbeidssøkerperiode for bruker")
                    call.respond(HttpStatusCode.OK, "Avsluttet arbeidssøkerperiode")
                }
            }
        }
    }

    route("/veileder/arbeidssoker") {
        authenticate("azure") {
            route("/perioder") {
                get {
                    logger.info("Henter arbeidssøkerperioder til NAV-ansatt")
                    call.respond(HttpStatusCode.OK, "Perioder")
                }
            }
        }
    }
}
