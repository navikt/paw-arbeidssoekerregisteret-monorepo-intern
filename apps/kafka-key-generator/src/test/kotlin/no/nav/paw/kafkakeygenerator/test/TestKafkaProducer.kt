package no.nav.paw.kafkakeygenerator.test

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.config.KAFKA_TOPOLOGY_CONFIG
import no.nav.paw.kafkakeygenerator.config.KafkaTopologyConfig
import no.nav.paw.kafkakeygenerator.utils.buildApplicationLogger
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

private val logger = buildApplicationLogger

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
    val kafkaTopologyConfig = loadNaisOrLocalConfiguration<KafkaTopologyConfig>(KAFKA_TOPOLOGY_CONFIG)

    val kafkaFactory = KafkaFactory(kafkaConfig)
    val kafkaProducer = kafkaFactory.createProducer<Long, Hendelse>(
        clientId = "${kafkaTopologyConfig.consumerGroupId}-producer",
        keySerializer = LongSerializer::class,
        valueSerializer = HendelseSerializer::class
    )

    val fraArbeidssoekerId = ArbeidssoekerId(2)
    val tilArbeidssoekerId = ArbeidssoekerId(1)
    val identitetsnummer1 = Identitetsnummer("02017012345")
    val identitetsnummer2 = Identitetsnummer("06017012345")

    val key = 1L
    val value = TestData.getIdentitetsnummerSammenslaatt(
        identitetsnummerList = listOf(identitetsnummer1, identitetsnummer2),
        fraArbeidssoekerId = fraArbeidssoekerId,
        tilArbeidssoekerId = tilArbeidssoekerId
    )

    try {
        logger.info("Sender hendelse {}", value)
        kafkaProducer.send(ProducerRecord(kafkaTopologyConfig.hendelseloggTopic, key, value)).get()
    } catch (e: Exception) {
        logger.error("Send hendelse feilet", e)
    } finally {
        kafkaProducer.close()
    }
}
