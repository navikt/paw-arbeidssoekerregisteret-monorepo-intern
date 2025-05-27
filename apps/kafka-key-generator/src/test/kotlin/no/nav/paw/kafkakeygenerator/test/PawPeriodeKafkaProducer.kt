package no.nav.paw.kafkakeygenerator.test

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.config.APPLICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

private val logger = buildApplicationLogger

class PeriodeAvroSerializer : SpecificAvroSerializer<Periode>()

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    with(applicationConfig.pawPeriodeConsumer) {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        val kafkaProducer = kafkaFactory.createProducer<Long, Periode>(
            clientId = "${groupId}-producer",
            keySerializer = LongSerializer::class,
            valueSerializer = PeriodeAvroSerializer::class
        )

        val records: List<ProducerRecord<Long, Periode>> = listOf(
            ProducerRecord(topic, TestData.key1, TestData.periode1_1),
            ProducerRecord(topic, TestData.key2, TestData.periode2_1),
            ProducerRecord(topic, TestData.key2, TestData.periode2_2),
            ProducerRecord(topic, TestData.key3, TestData.periode3_1),
            ProducerRecord(topic, TestData.key3, TestData.periode3_2),
            ProducerRecord(topic, TestData.key4, TestData.periode4_1),
            ProducerRecord(topic, TestData.key4, TestData.periode4_2),
            ProducerRecord(topic, TestData.key5, TestData.periode5_1),
            ProducerRecord(topic, TestData.key5, TestData.periode5_2)
        )

        try {
            records.forEach { record ->
                logger.info("Sender key {} value {}", record.key(), record.value())
                kafkaProducer.send(record).get()
            }
        } catch (e: Exception) {
            logger.error("Send periode feilet", e)
        } finally {
            kafkaProducer.close()
        }
    }
}
