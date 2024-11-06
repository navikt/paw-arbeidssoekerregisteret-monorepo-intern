package no.nav.paw.config.kafka.streams

import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.TimestampedKeyValueStore
import org.apache.kafka.streams.state.ValueAndTimestamp
import java.time.Duration
import java.time.Instant

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
            function = { wallclock , context ->
                val store = context.getStateStore<TimestampedKeyValueStore<K, V>>(name)
                store.all()
                    .use { iterator ->
                        val limit = wallclock - duration
                        iterator.forEach { (key, timestamp, value) ->
                            if (timestamp.isBefore(limit)) {
                                context.forward(
                                    Record(key, value, timestamp.toEpochMilli())
                                )
                                store.delete(key)
                            }
                        }
                    }
            }
        ),
        function = { record ->
            val store = getStateStore<TimestampedKeyValueStore<K, V>>(name)
            val timestamp = store.get(record.key())?.timestamp() ?: record.timestamp()
            store.put(
                record.key(),
                ValueAndTimestamp.make(record.value(), timestamp)
            )
        },
        stateStoreNames = listOf(name).toTypedArray()
    )

operator fun <K, V> KeyValue<K, ValueAndTimestamp<V>>.component1(): K = this.key
operator fun <K, V> KeyValue<K, ValueAndTimestamp<V>>.component2(): Instant = Instant.ofEpochMilli(value.timestamp())
operator fun <K, V> KeyValue<K, ValueAndTimestamp<V>>.component3(): V = value.value()
