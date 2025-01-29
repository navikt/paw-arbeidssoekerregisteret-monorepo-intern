package no.nav.paw.dolly.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import no.nav.paw.error.plugin.ErrorHandlingPlugin

fun Application.configureHTTP() {
    install(IgnoreTrailingSlash)
    install(ErrorHandlingPlugin)
}