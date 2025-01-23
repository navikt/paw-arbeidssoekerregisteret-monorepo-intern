package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import io.opentelemetry.api.trace.Span
import java.time.Instant

data class Person(
    val personId: Long,
    val recordKey: String,
    val sistEndret: Instant,
    val tidspunktFraKilde: Instant,
    val traceparant: String = Span.current().spanContext.let { ctx ->
        "00-${ctx.traceId}-${ctx.spanId}-${ctx.traceFlags.asHex()}"
    }
)