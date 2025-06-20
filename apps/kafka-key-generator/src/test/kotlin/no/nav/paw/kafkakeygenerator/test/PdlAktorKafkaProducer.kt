package no.nav.paw.kafkakeygenerator.test

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.config.APPLICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.test.TestData.aktor
import no.nav.paw.kafkakeygenerator.test.TestData.identifikator
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
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

        val aktor1 = "2647237114816" to aktor(
            listOf(
                identifikator(ident = "05507646184", type = Type.FOLKEREGISTERIDENT, gjeldende = true),
                identifikator(ident = "13497632174", type = Type.FOLKEREGISTERIDENT, gjeldende = false),
                identifikator(ident = "02507637593", type = Type.FOLKEREGISTERIDENT, gjeldende = true),
                identifikator(ident = "18497638182", type = Type.FOLKEREGISTERIDENT, gjeldende = false),
                identifikator(ident = "2002308243366", type = Type.AKTORID, gjeldende = false),
                identifikator(ident = "2647237114816", type = Type.AKTORID, gjeldende = true)
            )
        )
        val aktor2 = "2078784314389" to aktor(
            listOf(
                identifikator(ident = "13497632174", type = Type.FOLKEREGISTERIDENT, gjeldende = true),
                identifikator(ident = "2078784314389", type = Type.AKTORID, gjeldende = true)
            )
        )
        val aktors = listOf(aktor1)
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
