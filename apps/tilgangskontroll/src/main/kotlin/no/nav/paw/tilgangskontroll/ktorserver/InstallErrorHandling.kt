package no.nav.paw.tilgangskontroll.ktorserver

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.error.plugin.ErrorHandlingPlugin

fun Application.installErrorHandling() {
    install(ErrorHandlingPlugin)
}