package no.nav.paw.logging.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import no.nav.paw.logging.logger.buildServerLogger
import org.slf4j.Logger
import java.util.*

const val CALL_ID_HEADER = "x_callId"
const val ADDITIONAL_CALL_ID_HEADER = "x_call-id"
val CALL_ID_HEADERS = listOf(CALL_ID_HEADER, ADDITIONAL_CALL_ID_HEADER)
const val CALL_ID_MDC = "x_callId"

fun Application.installLoggingPlugin(
    logger: Logger = buildServerLogger,
) {
    install(CallId) {
        CALL_ID_HEADERS.forEach { retrieveFromHeader(it) }
        generate { UUID.randomUUID().toString() }
        verify { it.isNotEmpty() }
    }
    install(CallLogging) {
        callIdMdc(CALL_ID_MDC)
        disableDefaultColors()
        filter { !it.request.path().startsWith("/internal") }
        this.logger = logger
    }
}
