package no.nav.paw.kafkakeygenerator.test

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.felles.model.ArbeidssoekerId
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.config.APPLICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

private val logger = buildApplicationLogger

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    with(applicationConfig) {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        val pawHendelseKafkaProducer = kafkaFactory.createProducer<Long, Hendelse>(
            clientId = pawHendelseloggProducer.clientId,
            keySerializer = LongSerializer::class,
            valueSerializer = HendelseSerializer::class
        )

        val fraArbeidssoekerId = ArbeidssoekerId(2)
        val tilArbeidssoekerId = ArbeidssoekerId(1)
        val identitetsnummer1 = Identitetsnummer("01017012345")
        val identitetsnummer2 = Identitetsnummer("02017012345")

        val key = 1L
        val value = TestData.identitetsnummerSammenslaattHendelse(
            identitetsnummerList = listOf(identitetsnummer1, identitetsnummer2),
            fraArbeidssoekerId = fraArbeidssoekerId,
            tilArbeidssoekerId = tilArbeidssoekerId
        )

        try {
            logger.info("Sender hendelse {}", value)
            pawHendelseKafkaProducer.send(ProducerRecord(pawHendelseloggProducer.topic, key, value)).get()
        } catch (e: Exception) {
            logger.error("Send hendelse feilet", e)
        } finally {
            pawHendelseKafkaProducer.close()
        }
    }
}
