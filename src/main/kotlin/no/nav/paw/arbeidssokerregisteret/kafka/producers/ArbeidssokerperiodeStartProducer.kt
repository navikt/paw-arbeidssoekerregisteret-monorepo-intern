package no.nav.paw.arbeidssokerregisteret.kafka.producers

import no.nav.paw.arbeidssokerregisteret.intern.StartV1
import no.nav.paw.arbeidssokerregisteret.utils.logger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

class ArbeidssokerperiodeStartProducer(
    private val kafkaProducerClient: KafkaProducer<String, StartV1>,
    private val topic: String
) {
    fun publish(value: StartV1) {
        val record: ProducerRecord<String, StartV1> = ProducerRecord(
            topic,
            null,
            UUID.randomUUID().toString(),
            value
        )

        kafkaProducerClient.send(record).get()
        logger.info("Sendte melding om start av arbeidssøkerperiode til $topic")
    }
}
