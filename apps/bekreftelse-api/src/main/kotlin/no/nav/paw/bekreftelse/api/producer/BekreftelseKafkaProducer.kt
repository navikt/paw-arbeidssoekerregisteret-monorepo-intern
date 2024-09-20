package no.nav.paw.bekreftelse.api.producer

import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.utils.buildBekreftelseSerde
import no.nav.paw.bekreftelse.api.utils.logger
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.config.kafka.sendDeferred
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

class BekreftelseKafkaProducer(
    private val applicationConfig: ApplicationConfig,
) {
    private lateinit var producer: Producer<Long, Bekreftelse>
    private val bekreftelseSerde = buildBekreftelseSerde()

    init {
        initializeProducer()
    }

    private fun initializeProducer() {
        val kafkaFactory = KafkaFactory(applicationConfig.kafkaClients)
        producer =
            kafkaFactory.createProducer<Long, Bekreftelse>(
                clientId = applicationConfig.kafkaTopology.producerId,
                keySerializer = LongSerializer::class,
                valueSerializer = bekreftelseSerde.serializer()::class
            )
    }

    suspend fun produceMessage(key: Long, message: Bekreftelse) {
        val topic = applicationConfig.kafkaTopology.bekreftelseTopic
        val record = ProducerRecord(topic, key, message)
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: offset={}", recordMetadata.offset())
    }

    fun closeProducer() {
        producer.close()
    }
}
