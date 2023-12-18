package no.nav.paw.arbeidssokerregisteret.kafka.producers

import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.utils.logger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class ArbeidssokerperiodeStartProducer(
    private val kafkaProducerClient: KafkaProducer<Long, Startet>,
    private val topic: String
) {
    fun publish(key: Long, value: Startet) {
        val record: ProducerRecord<Long, Startet> = ProducerRecord(
            topic,
            null,
            key,
            value
        )

        kafkaProducerClient.send(record).get()
        logger.info("Sendte melding om start av arbeidssøkerperiode til $topic")
    }
}
