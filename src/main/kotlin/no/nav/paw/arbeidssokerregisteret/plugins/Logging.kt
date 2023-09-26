package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.path
import java.util.*

fun Application.configureLogging() {
    install(CallId) {
        retrieveFromHeader("x_callId")
        generate { UUID.randomUUID().toString() }
        verify { it.isNotEmpty() }
    }
    install(CallLogging) {
        callIdMdc("x_callId")
        disableDefaultColors()
        filter { !it.request.path().startsWith("/internal") }
    }
}