package no.nav.paw.kafkakeygenerator.test

import io.confluent.kafka.serializers.KafkaAvroSerializer
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

private val logger = buildApplicationLogger

class AktorAvroSerializer : SpecificAvroSerializer<Aktor>()

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    with(applicationConfig.pdlAktorConsumer) {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        val pawHendelseKafkaProducer = kafkaFactory.createProducer<Any, Aktor>(
            clientId = "${groupId}-producer",
            keySerializer = KafkaAvroSerializer::class,
            valueSerializer = AktorAvroSerializer::class
        )

        val aktor1 = TestData.aktorId8_1.identitet to TestData.aktor8_1
        val aktor2 = TestData.aktorId8_2.identitet to TestData.aktor8_2
        val aktor3 = TestData.aktorId8_2.identitet to TestData.aktor8_3

        val aktors = listOf(aktor1, aktor2, aktor3)
        val records: List<ProducerRecord<Any, Aktor>> = aktors.map {
            ProducerRecord(topic, it.first, it.second)
        }

        try {
            records.forEach { record ->
                logger.info("Sender key {} value {}", record.key(), record.value())
                pawHendelseKafkaProducer.send(record).get()
            }
        } catch (e: Exception) {
            logger.error("Send hendelse feilet", e)
        } finally {
            pawHendelseKafkaProducer.close()
        }
    }
}
