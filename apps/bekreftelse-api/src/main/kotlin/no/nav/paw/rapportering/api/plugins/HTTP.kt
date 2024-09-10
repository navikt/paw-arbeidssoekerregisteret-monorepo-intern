package no.nav.paw.rapportering.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.IgnoreTrailingSlash

fun Application.configureHTTP() {
    install(IgnoreTrailingSlash)
    install(StatusPages)
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
