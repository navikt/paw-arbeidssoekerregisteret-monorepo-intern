package no.nav.paw.config.kafka

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

fun <K, V> Producer<K, V>.sendDeferred(record: ProducerRecord<K, V>): Deferred<RecordMetadata> {
    val deferred = CompletableDeferred<RecordMetadata>()
    send(record) { metadata, exception ->
        if (exception != null) {
            deferred.completeExceptionally(exception)
        } else {
            deferred.complete(metadata)
        }
    }
    return deferred
}
