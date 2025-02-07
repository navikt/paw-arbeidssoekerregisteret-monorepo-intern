package no.nav.paw.dolly.api.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.opentelemetry.api.trace.Span

fun Application.installTracingPlugin() {
    install(
        createApplicationPlugin("OtelTraceIdPlugin") {
            onCallRespond { call, _ ->
                runCatching { Span.current().spanContext.traceId }
                    .onSuccess { call.response.headers.append("x-trace-id", it) }
            }
        }
    )
}