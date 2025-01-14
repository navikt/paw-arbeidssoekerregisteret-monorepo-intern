package no.nav.paw.kafka.consumer

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

fun <K, V> Consumer<K, V>.asSequence(
    stop: AtomicBoolean,
    pollTimeout: Duration = Duration.ofMillis(500),
    closeTimeout: Duration = Duration.ofMillis(250)
): Sequence<ConsumerRecords<K, V>> =
    generateSequence {
        if (stop.get()) {
            close(closeTimeout)
            null
        } else {
            poll(pollTimeout)
        }
    }
