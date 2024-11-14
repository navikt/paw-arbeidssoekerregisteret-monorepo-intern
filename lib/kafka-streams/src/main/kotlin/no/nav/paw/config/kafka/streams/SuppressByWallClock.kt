package no.nav.paw.config.kafka.streams

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.TimestampedKeyValueStore
import org.apache.kafka.streams.state.ValueAndTimestamp
import java.time.Duration
import java.time.Instant

private const val TRACE_PARENT = "traceparent"

fun <K, V> KStream<K, V>.supressByWallClock(
    name: String,
    duration: Duration,
    checkInterval: Duration = Duration.ofMinutes(10)
): KStream<K, V> =
    genericProcess(
        name = name,
        punctuation = Punctuation(
            interval = checkInterval,
            type = PunctuationType.WALL_CLOCK_TIME,
            function = { wallclock, context ->
                val store = context.getStateStore<TimestampedKeyValueStore<K, V>>(name)
                val storeTraceId = context.getStateStore<KeyValueStore<K, String>>("${name}_trace_id")
                store.all()
                    .use { iterator ->
                        val limit = wallclock - duration
                        iterator.forEach { (key, timestamp, value) ->
                            if (timestamp.isBefore(limit)) {
                                val traceparent = storeTraceId.get(key)
                                val span = traceparent
                                    ?.let { traceParent ->
                                        createSpanFromTraceparent(traceParent)
                                    } ?: Span.current()
                                try {
                                    context.forward(
                                        Record(key, value, timestamp.toEpochMilli())
                                            .let { record ->
                                                traceparent?.let { tp ->
                                                    record.withHeaders(
                                                        RecordHeaders(
                                                            arrayOf(RecordHeader(TRACE_PARENT, tp.toByteArray()))
                                                        )
                                                    )
                                                } ?: record
                                            }
                                    )
                                    store.delete(key)
                                    storeTraceId.delete(key)
                                } finally {
                                    span.end()
                                }
                            }
                        }
                    }
            }
        ),
        function = { record ->
            val store = getStateStore<TimestampedKeyValueStore<K, V>>(name)
            val storeTraceId = getStateStore<KeyValueStore<K, String>>("${name}_trace_id")
            val timestamp = store.get(record.key())?.timestamp() ?: record.timestamp()
            store.put(
                record.key(),
                ValueAndTimestamp.make(record.value(), timestamp)
            )
            storeTraceId.put(record.key(), record.headers().lastHeader(TRACE_PARENT)?.value()?.toString(Charsets.UTF_8))
        },
        stateStoreNames = listOf(name, "${name}_trace_id").toTypedArray()
    )

operator fun <K, V> KeyValue<K, ValueAndTimestamp<V>>.component1(): K = this.key
operator fun <K, V> KeyValue<K, ValueAndTimestamp<V>>.component2(): Instant = Instant.ofEpochMilli(value.timestamp())
operator fun <K, V> KeyValue<K, ValueAndTimestamp<V>>.component3(): V = value.value()

fun createSpanFromTraceparent(traceparent: String): Span {
    val propagator: TextMapPropagator = GlobalOpenTelemetry.getPropagators().textMapPropagator
    val context: Context = propagator.extract(Context.current(), traceparent, object : TextMapGetter<String> {
        override fun keys(carrier: String): Iterable<String> = listOf(TRACE_PARENT)
        override fun get(carrier: String?, key: String): String? = carrier
    })

    val spanContext = Span.fromContext(context).spanContext
    val tracer = GlobalOpenTelemetry.getTracer("custom-tracer")
    return tracer.spanBuilder("forward aktor_v2")
        .setParent(Context.current().with(Span.wrap(spanContext)))
        .startSpan()
}
