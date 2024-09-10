package no.nav.paw.rapportering.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.opentelemetry.api.trace.Span


fun Application.configureOtel() {
    install(
        createApplicationPlugin("OtelTraceIdPlugin") {
            onCallRespond { call, _ ->
                runCatching { Span.current().spanContext.traceId }
                    .onSuccess { call.response.headers.append("x-trace-id", it) }
            }
        }
    )
}