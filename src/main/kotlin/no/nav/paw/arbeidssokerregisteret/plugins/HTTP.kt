package no.nav.paw.arbeidssokerregisteret.plugins

import com.fasterxml.jackson.databind.DatabindException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode
import no.nav.paw.arbeidssokerregisteret.domain.http.Feil
import no.nav.paw.arbeidssokerregisteret.services.RemoteServiceException
import no.nav.paw.arbeidssokerregisteret.utils.logger

fun Application.configureHTTP() {
    install(IgnoreTrailingSlash)
    install(StatusPages) {
       exception<Throwable> { call, cause ->
            when (cause) {
                is ContentTransformationException -> {
                    logger.debug("Bad request", cause)
                    call.respond(HttpStatusCode.BadRequest, Feil(cause.message ?: "Bad request", Feilkode.FEIL_VED_LESING_AV_FORESPORSEL))
                }
                is DatabindException -> {
                    logger.debug("Bad request", cause)
                    call.respond(HttpStatusCode.BadRequest, Feil(cause.message ?: "Bad request", Feilkode.FEIL_VED_LESING_AV_FORESPORSEL))
                }
                is RemoteServiceException -> {
                    logger.warn("Request failed with status: ${cause}. Description: ${cause.message}")
                    call.respond(cause.status, Feil(cause.message ?: "ukjent feil knyttet til eksternt system", cause.feilkode))
                }
                is StatusException -> {
                    logger.error("Request failed with status: ${cause}. Description: ${cause.message}")
                    call.respond(cause.status, Feil(cause.message ?: "ukjent feil", cause.feilkode))
                }
                else -> {
                    logger.error("Request failed with status: ${cause}. Description: ${cause.message}")
                    call.respond(HttpStatusCode.InternalServerError, Feil(cause.message ?: "ukjent feil", Feilkode.UKJENT_FEIL))
                }
            }
        }
    }
    install(CORS) {
        anyHost()

        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowMethod(io.ktor.http.HttpMethod.Delete)

        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.AccessControlAllowOrigin)

        allowHeadersPrefixed("nav-")
    }
}
