package no.nav.paw.tracing

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import org.slf4j.LoggerFactory

private val spanHandlerLogger = LoggerFactory.getLogger("spanHandler")
fun initSpan(
    traceparent: String,
    instrumentationScopeName: String,
    spanName: String
): ClosableSpan {
    spanHandlerLogger.info("traceparent: {}", traceparent)
    return traceparent.split("-")
        .takeIf { it.size == 4 }
        ?.let { asArray ->
            SpanContext.createFromRemoteParent(
                asArray[1],
                asArray[2],
                TraceFlags.getSampled(),
                TraceState.getDefault()
            )
        }?.let { spanContext ->
            val spanNoop = Span.wrap(spanContext)
            val originalSPan = Span.current()
            originalSPan.addLink(spanContext)
            val telemetry = GlobalOpenTelemetry.get()
            val tracer = telemetry.tracerProvider.get(instrumentationScopeName)
            tracer.spanBuilder(spanName)
                .setParent(Context.current().with(spanNoop))
                .startSpan()
                .also { it.makeCurrent() }
                .let { ClosableSpan(it, originalSPan) }
        } ?: ClosableSpan(null, null)
}

class ClosableSpan(span: Span?, private val replacedSpan: Span?) : AutoCloseable, Span by (span ?: Span.getInvalid()) {
    override fun close() {
        end()
        replacedSpan?.makeCurrent()
    }
}