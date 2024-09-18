package no.nav.paw.bekreftelse.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.IgnoreTrailingSlash
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.AutorisasjonConfig
import no.nav.paw.config.env.NaisEnv
import no.nav.paw.error.handler.handleException

fun Application.configureHTTP(applicationConfig: ApplicationConfig) {
    install(IgnoreTrailingSlash)
    install(StatusPages) {
        exception<Throwable> { call: ApplicationCall, cause: Throwable ->
            call.handleException(cause)
        }
    }
    install(CORS) {
        val origins = applicationConfig.autorisasjon.getCorsAllowOrigins()

        when (applicationConfig.naisEnv) {
            NaisEnv.ProdGCP -> {
                origins.forEach { allowHost(it) }
            }

            NaisEnv.DevGCP -> {
                origins.forEach { allowHost(it) }
            }

            NaisEnv.Local -> anyHost()
        }

        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Head)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)

        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.AccessControlAllowOrigin)

        allowHeadersPrefixed("nav-")
    }
}

private fun AutorisasjonConfig.getCorsAllowOrigins() =
    corsAllowOrigins?.let { origins ->
        origins.split(",")
            .map { origin -> origin.trim() }
    } ?: emptyList()
