package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import no.nav.paw.arbeidssokerregisteret.utils.logger

fun Application.configureHTTP() {
    install(IgnoreTrailingSlash)
    install(StatusPages) {
        exception<StatusException> { call, cause ->
            logger.error("Request failed with status: ${cause.status}. Description: ${cause.description}")
            call.respond(cause.status, cause.status)
        }
        exception<Throwable> { call, cause ->
            logger.info("Feil ved kall", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                cause.message ?: HttpStatusCode.InternalServerError.description
            )
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
