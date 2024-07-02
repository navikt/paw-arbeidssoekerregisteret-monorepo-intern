package no.nav.paw.arbeidssokerregisteret.plugins

import com.fasterxml.jackson.databind.DatabindException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feil
import no.nav.paw.arbeidssokerregisteret.routes.kanStarte
import no.nav.paw.arbeidssokerregisteret.routes.opplysninger
import no.nav.paw.arbeidssokerregisteret.routes.periode
import no.nav.paw.arbeidssokerregisteret.routes.startStopApi
import no.nav.paw.arbeidssokerregisteret.services.RemoteServiceException
import no.nav.paw.arbeidssokerregisteret.utils.logger
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Feil as OpplysningerFeil
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feil as StartStoppFeil

fun ApplicationCall.isStartStopRequest(): Boolean = request.path().startsWith("$startStopApi$periode")
fun ApplicationCall.isKanStarteRequest(): Boolean = request.path().startsWith("$startStopApi$kanStarte")
fun ApplicationCall.isOpplysningerMottattRequest(): Boolean = request.path().startsWith("$startStopApi$opplysninger")

fun Application.configureHTTP() {
    install(IgnoreTrailingSlash)
    install(CORS) {
        anyHost()

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)

        allowHeadersPrefixed("nav-")
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is ContentTransformationException,
                is DatabindException,
                is BadRequestException -> {
                    logger.debug("Bad request", cause)
                    when {
                        call.isStartStopRequest() ||
                                call.isKanStarteRequest() -> call.respond(
                            HttpStatusCode.BadRequest,
                            StartStoppFeil(
                                melding = cause.message ?: "Bad request",
                                feilKode = StartStoppFeil.FeilKode.FEIL_VED_LESING_AV_FORESPORSEL
                            )
                        )

                        call.isOpplysningerMottattRequest() -> call.respond(
                            HttpStatusCode.BadRequest,
                            OpplysningerFeil(
                                melding = cause.message ?: "Bad request",
                                feilKode = OpplysningerFeil.FeilKode.FEIL_VED_LESING_AV_FORESPORSEL
                            )
                        )

                        else -> call.respond(HttpStatusCode.BadRequest)
                    }
                }

                is RemoteServiceException -> {
                    logger.warn("Request failed with status: ${cause}. Description: ${cause.message}")
                    when {
                        call.isStartStopRequest() ||
                                call.isKanStarteRequest() -> call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            StartStoppFeil(
                                melding = cause.message ?: "Remote call failed",
                                feilKode = StartStoppFeil.FeilKode.UVENTET_FEIL_MOT_EKSTERN_TJENESTE
                            )
                        )

                        call.isOpplysningerMottattRequest() -> call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            OpplysningerFeil(
                                melding = cause.message ?: "Remote call failed",
                                feilKode = OpplysningerFeil.FeilKode.UVENTET_FEIL_MOT_EKSTERN_TJENESTE
                            )
                        )

                        else -> call.respond(HttpStatusCode.BadRequest)
                    }
                }

                is StatusException -> {
                    logger.error("Request failed with status: ${cause}. Description: ${cause.message}")
                    when {
                        call.isStartStopRequest() ||
                                call.isKanStarteRequest() -> call.respond(
                            cause.status,
                            StartStoppFeil(
                                melding = cause.message ?: "Unknown error",
                                feilKode = cause.feilkode.startStoppFeilkode()
                            )
                        )

                        call.isOpplysningerMottattRequest() -> call.respond(
                            cause.status,
                            OpplysningerFeil(
                                melding = cause.message ?: "Unknown error",
                                feilKode = cause.feilkode.opplysningerFeilkode()
                            )
                        )

                        else -> call.respond(HttpStatusCode.BadRequest)
                    }
                }

                else -> {
                    logger.error("Request failed with status: ${cause}. Description: ${cause.message}", cause)
                    when {
                        call.isStartStopRequest() ||
                                call.isKanStarteRequest() -> call.respond(
                            HttpStatusCode.InternalServerError,
                            StartStoppFeil(
                                melding = cause.message ?: "Unknown error",
                                feilKode = Feil.FeilKode.UKJENT_FEIL
                            )
                        )

                        call.isOpplysningerMottattRequest() -> call.respond(
                            HttpStatusCode.InternalServerError,
                            OpplysningerFeil(
                                melding = cause.message ?: "Unknown error",
                                feilKode = OpplysningerFeil.FeilKode.UKJENT_FEIL
                            )
                        )

                        else -> call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
