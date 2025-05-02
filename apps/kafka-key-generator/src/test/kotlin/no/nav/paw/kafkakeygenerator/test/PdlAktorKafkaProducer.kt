package no.nav.paw.kafkakeygenerator.test

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.config.APPLICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

private val logger = buildApplicationLogger

class AktorAvroSerializer : SpecificAvroSerializer<Aktor>()

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    with(applicationConfig) {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        val pawHendelseKafkaProducer = kafkaFactory.createProducer<String, Aktor>(
            clientId = "${pdlAktorConsumer.groupId}-producer",
            keySerializer = StringSerializer::class,
            valueSerializer = AktorAvroSerializer::class
        )

        val key = "1"
        val value = TestData.aktor()

        try {
            logger.info("Sender hendelse {}", value)
            pawHendelseKafkaProducer.send(ProducerRecord(pdlAktorConsumer.topic, key, value)).get()
        } catch (e: Exception) {
            logger.error("Send hendelse feilet", e)
        } finally {
            pawHendelseKafkaProducer.close()
        }
    }
}
