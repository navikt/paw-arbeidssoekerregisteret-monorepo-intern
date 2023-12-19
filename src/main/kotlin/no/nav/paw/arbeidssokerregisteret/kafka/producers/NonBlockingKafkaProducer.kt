package no.nav.paw.arbeidssokerregisteret.kafka.producers

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.utils.logger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

class NonBlockingKafkaProducer<K, V>(
    private val kafkaProducerClient: KafkaProducer<K, V>
) {
    fun send(record: ProducerRecord<K, V>): Deferred<RecordMetadata> {
        val deferred = CompletableDeferred<RecordMetadata>()
        kafkaProducerClient.send(record) { metadata, exception ->
            if (exception != null) {
                deferred.completeExceptionally(exception)
            } else {
                deferred.complete(metadata)
            }
        }
        return deferred
    }
}


suspend fun Deferred<RecordMetadata>.awaitAndLog(messageDescription: String) {
    val recordMetadata = await()
    logger.trace(
        "Sendte meldingen: beskrivelse={}, topic={}, partition={}, offset={}",
        messageDescription,
        recordMetadata.topic(),
        recordMetadata.partition(),
        recordMetadata.offset()
    )
}
