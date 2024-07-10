package no.nav.paw.arbeidssoekerregisteret.plugins.tracing

import io.ktor.server.application.createApplicationPlugin
import io.opentelemetry.api.trace.Span

val OpenTelemetryPlugin = createApplicationPlugin("OpenTelemetryPlugin") {
    onCallRespond { call, _ ->
        runCatching { Span.current().spanContext.traceId }
            .onSuccess { call.response.headers.append("x-trace-id", it) }
    }
}