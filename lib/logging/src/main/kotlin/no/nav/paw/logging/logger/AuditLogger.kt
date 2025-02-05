package no.nav.paw.logging.logger

import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val buildAuditLogger: AuditLogger get() = AuditLogger.getLogger()

class AuditLogger private constructor(
    private val logger: Logger = LoggerFactory.getLogger("AuditLogger"),
    private val log: (String) -> Unit = logger::info // Endre ønsket loggnivå her
) {
    fun audit(
        melding: String,
        aktorIdent: String,
        sluttbrukerIdent: String,
        event: CefMessageEvent = CefMessageEvent.ACCESS,
        runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment,
    ) {
        val message = CefMessage.builder()
            .applicationName(runtimeEnvironment.appNameOrDefaultForLocal())
            .event(event)
            .name("Sporingslogg")
            .severity(CefMessageSeverity.INFO)
            .sourceUserId(aktorIdent)
            .destinationUserId(sluttbrukerIdent)
            .timeEnded(System.currentTimeMillis())
            .extension("msg", melding)
            .build()
            .toString()
        log(message)
    }

    companion object {
        fun getLogger(): AuditLogger = AuditLogger()
    }
}
