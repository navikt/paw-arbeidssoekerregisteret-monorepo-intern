package no.nav.paw.bekreftelse.api.utils

import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.paw.bekreftelse.api.model.NavAnsatt
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.poao_tilgang.client.TilgangType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T> T.logger: Logger get() = LoggerFactory.getLogger(T::class.java)

inline val auditLogger: Logger get() = LoggerFactory.getLogger("AuditLogger")

fun Logger.audit(
    runtimeEnvironment: RuntimeEnvironment,
    identitetsnummer: String,
    navAnsatt: NavAnsatt,
    tilgangType: TilgangType,
    melding: String,
) {
    val message = CefMessage.builder()
        .applicationName(runtimeEnvironment.appNameOrDefaultForLocal())
        .event(if (tilgangType == TilgangType.LESE) CefMessageEvent.ACCESS else CefMessageEvent.UPDATE)
        .name("Sporingslogg")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navAnsatt.navIdent)
        .destinationUserId(identitetsnummer)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", melding)
        .build()
        .toString()
    this.info(message)
}