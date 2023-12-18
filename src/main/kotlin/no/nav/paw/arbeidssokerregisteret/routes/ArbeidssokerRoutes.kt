package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.services.ArbeidssokerService
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.utils.getPidClaim
import no.nav.paw.arbeidssokerregisteret.utils.logger

fun Route.arbeidssokerRoutes(arbeidssokerService: ArbeidssokerService, autorisasjonService: AutorisasjonService) {
    route("/api/v1") {
        authenticate("tokenx") {
            route("/arbeidssoker") {
                route("/kan-registreres") {
                    get {
                        logger.info("Sjekker om bruker kan registreres som arbeidssøker")

                        val foedselsnummer = call.getPidClaim()

                        val kanRegistreres = arbeidssokerService.kanRegistreresSomArbeidssoker(foedselsnummer)

                        logger.info("Bruker kan registreres som arbeidssøker: $kanRegistreres")

                        call.respond(HttpStatusCode.OK, kanRegistreres)
                    }
                }
                route("/perioder") {
                    post {
                        logger.info("Starter ny arbeidssøkerperiode for bruker")

                        val foedselsnummer = call.getPidClaim()

                        arbeidssokerService.startArbeidssokerperiode(
                            foedselsnummer = foedselsnummer,
                            utfoertAv = Bruker(
                                id = foedselsnummer.verdi,
                                type = BrukerType.SLUTTBRUKER
                            )
                        )

                        logger.info("Startet arbeidssøkerperiode for bruker")

                        call.respond(HttpStatusCode.Accepted)
                    }

                    put {
                        logger.info("Avslutter arbeidssøkerperiode for bruker")

                        val foedselsnummer = call.getPidClaim()
                        arbeidssokerService.avsluttArbeidssokerperiode(foedselsnummer.verdi)

                        logger.info("Avsluttet arbeidssøkerperiode for bruker")

                        call.respond(HttpStatusCode.Accepted)
                    }
                }
            }
        }

//        route("/veileder/arbeidssoker/perioder") {
//            authenticate("azure") {
//                post {
//                    logger.info("Veileder starter ny arbeidssøkerperiode for bruker")
//
//                    val foedselsnummer = call.receive<VeilederRequest>().getFoedselsnummer()
//                    val navAnsatt = call.getNavAnsatt()
//
//                    val harNavBrukerTilgang =
//                        autorisasjonService.verifiserVeilederTilgangTilBruker(navAnsatt, foedselsnummer)
//
//                    if (!harNavBrukerTilgang) {
//                        return@post call.respond(HttpStatusCode.Forbidden, "NAV-ansatt har ikke tilgang")
//                    }
//
//                    arbeidssokerService.startArbeidssokerperiode(foedselsnummer, navAnsatt.ident)
//
//                    logger.info("Veileder startet arbeidssøkerperiode for bruker")
//
//                    call.respond(HttpStatusCode.Accepted)
//                }
//            }
//        }
    }
}
