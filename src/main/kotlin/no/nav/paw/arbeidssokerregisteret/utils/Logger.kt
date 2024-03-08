package no.nav.paw.arbeidssokerregisteret.utils

import ch.qos.logback.core.util.OptionHelper.getEnv
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import org.slf4j.LoggerFactory

inline val <reified T : Any> T.logger get() = LoggerFactory.getLogger(T::class.java.name)

inline val autitLogger get() = LoggerFactory.getLogger("AuditLogger")
fun auditLogMessage(identietsnummer: Identitetsnummer, navAnsatt: NavAnsatt, melding: String): String =
    CefMessage.builder()
        .applicationName(getEnv("NAIS_APP_NAME") ?: "paw-arbeidssokerregisteret-api-inngang")
        .event(CefMessageEvent.ACCESS)
        .name("Sporingslogg")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navAnsatt.ident)
        .destinationUserId(identietsnummer.verdi)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", melding)
        .build()
        .toString()
