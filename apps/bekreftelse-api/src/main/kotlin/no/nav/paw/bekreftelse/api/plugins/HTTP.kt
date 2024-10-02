package no.nav.paw.bekreftelse.api.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.IgnoreTrailingSlash
import no.nav.paw.bekreftelse.api.config.AutorisasjonConfig
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.config.env.Local
import no.nav.paw.config.env.Nais
import no.nav.paw.error.handler.handleException

fun Application.configureHTTP(applicationContext: ApplicationContext) {
    install(IgnoreTrailingSlash)
    install(StatusPages) {
        exception<Throwable> { call: ApplicationCall, cause: Throwable ->
            call.handleException(cause)
        }
    }
    install(CORS) {
        val origins = applicationContext.applicationConfig.autorisasjon.getCorsAllowOrigins()

        when (applicationContext.serverConfig.runtimeEnvironment) {
            is Nais -> {
                origins.forEach { allowHost(it) }
            }

            is Local -> anyHost()
        }

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Head)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)

        allowCredentials = true

        allowHeadersPrefixed("nav-")
    }
}

private fun AutorisasjonConfig.getCorsAllowOrigins() =
    corsAllowOrigins?.let { origins ->
        origins.split(",")
            .map { origin -> origin.trim() }
    } ?: emptyList()
