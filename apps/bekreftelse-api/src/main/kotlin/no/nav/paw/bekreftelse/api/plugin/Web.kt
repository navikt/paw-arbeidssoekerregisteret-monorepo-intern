package no.nav.paw.bekreftelse.api.plugin

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.IgnoreTrailingSlash
import no.nav.paw.bekreftelse.api.config.AutorisasjonConfig
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.config.env.Local
import no.nav.paw.config.env.Nais

fun Application.installWebPlugins(applicationContext: ApplicationContext) {
    install(IgnoreTrailingSlash)
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
