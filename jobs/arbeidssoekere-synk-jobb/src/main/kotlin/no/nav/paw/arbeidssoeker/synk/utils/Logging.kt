package no.nav.paw.arbeidssoeker.synk.utils

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import org.slf4j.Logger

private fun attributes(status: HttpStatusCode): Attributes = Attributes.builder()
    .put("domain", "perioder")
    .put("action", "write")
    .put("status", status.value.toString())
    .build()

fun Logger.traceAndLog(status: HttpStatusCode) {
    with(Span.current()) {
        val attributes = attributes(status)
        if (status.isSuccess()) {
            setAllAttributes(attributes)
            addEvent("ok", attributes)
            setStatus(StatusCode.OK)
            debug("Opprettelse av periode fullførte OK")
        } else {
            setAllAttributes(attributes)
            addEvent("error", attributes)
            setStatus(StatusCode.ERROR, status.toString())
            debug("Opprettelse av periode feilet med status {}", status.value)
        }
    }
}
