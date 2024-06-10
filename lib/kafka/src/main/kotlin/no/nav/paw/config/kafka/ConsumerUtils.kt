package no.nav.paw.config.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
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
