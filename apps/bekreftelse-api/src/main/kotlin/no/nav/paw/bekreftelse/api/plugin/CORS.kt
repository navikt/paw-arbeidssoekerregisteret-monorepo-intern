package no.nav.paw.bekreftelse.api.plugin

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.AutorisasjonConfig
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.config.env.Local
import no.nav.paw.config.env.Nais

fun Application.installCorsPlugins(
    serverConfig: ServerConfig,
    applicationConfig: ApplicationConfig
) {
    install(CORS) {
        val origins = applicationConfig.autorisasjon.getCorsAllowOrigins()

        when (serverConfig.runtimeEnvironment) {
            is Nais -> origins.forEach { allowHost(it) }
            is Local -> anyHost()
        }

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Head)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeadersPrefixed("nav-")

        allowCredentials = true
    }
}

private fun AutorisasjonConfig.getCorsAllowOrigins() =
    corsAllowOrigins?.split(",")?.map(String::trim) ?: emptyList()
