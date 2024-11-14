package no.nav.paw.config.kafka.streams

import io.opentelemetry.api.trace.Span
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.TimestampedKeyValueStore
import org.apache.kafka.streams.state.ValueAndTimestamp
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

const val TRACE_PARENT = "traceparent"

private val suppressByWallBlockLogger = LoggerFactory.getLogger("suppressByWallBlockLogger")

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
                                suppressByWallBlockLogger.info("Loaded traceparent: {}", traceparent)
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
                            }
                        }
                    }
            }
        ),
        function = { record ->
            val store = getStateStore<TimestampedKeyValueStore<K, V>>(name)
            val storeTraceId = getStateStore<KeyValueStore<K, String>>("${name}_trace_id")
            val timestamp = store.get(record.key())?.timestamp() ?: record.timestamp()
            val traceparent = Span.current().spanContext.let { ctx ->
                "00-${ctx.traceId}-${ctx.spanId}-${ctx.traceFlags.asHex()}"
            }
            store.put(
                record.key(),
                ValueAndTimestamp.make(record.value(), timestamp)
            )
            suppressByWallBlockLogger.info("Storing traceparent: {}", traceparent)
            storeTraceId.put(
                record.key(),
                traceparent
            )
        },
        stateStoreNames = listOf(name, "${name}_trace_id").toTypedArray()
    )

operator fun <K, V> KeyValue<K, ValueAndTimestamp<V>>.component1(): K = this.key
operator fun <K, V> KeyValue<K, ValueAndTimestamp<V>>.component2(): Instant = Instant.ofEpochMilli(value.timestamp())
operator fun <K, V> KeyValue<K, ValueAndTimestamp<V>>.component3(): V = value.value()
