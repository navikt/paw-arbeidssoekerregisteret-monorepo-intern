package no.nav.paw.tilgangskontroll.ktorserver

import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import no.nav.paw.error.handler.handleException

fun Application.installStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call: ApplicationCall, cause: Throwable ->
            call.handleException(cause)
        }
    }
}