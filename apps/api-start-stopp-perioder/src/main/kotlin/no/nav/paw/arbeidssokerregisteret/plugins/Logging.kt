package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path

fun Application.configureLogging() {
    install(CallLogging) {
        disableDefaultColors()
        filter { !it.request.path().startsWith("/internal") }
    }
}
