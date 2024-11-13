package no.nav.paw.kafkakeygenerator.producer

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.kafkakeygenerator.config.KAFKA_TOPOLOGY_CONFIG
import no.nav.paw.kafkakeygenerator.config.KafkaTopologyConfig
import no.nav.paw.kafkakeygenerator.utils.buildApplicationLogger
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata as HendelseMetadata

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

    val fraArbeidssoekerId = 2L
    val tilArbeidssoekerId = 1L
    val identitetsnummer = "02017012345"

    val key = 1L
    val value = IdentitetsnummerSammenslaatt(
        id = fraArbeidssoekerId,
        hendelseId = UUID.randomUUID(),
        identitetsnummer = identitetsnummer,
        metadata = HendelseMetadata(
            tidspunkt = Instant.now(),
            utfoertAv = Bruker(type = BrukerType.SYSTEM, id = "paw"),
            kilde = "paw",
            aarsak = "test"
        ),
        alleIdentitetsnummer = listOf(identitetsnummer),
        flyttetTilArbeidssoekerId = tilArbeidssoekerId,
    )

    try {
        logger.info("Sender hendelse {}", value.id)
        kafkaProducer.send(ProducerRecord(kafkaTopologyConfig.hendelseloggTopic, key, value)).get()
    } catch (e: Exception) {
        logger.error("Send hendelse feilet", e)
    } finally {
        kafkaProducer.close()
    }
}
