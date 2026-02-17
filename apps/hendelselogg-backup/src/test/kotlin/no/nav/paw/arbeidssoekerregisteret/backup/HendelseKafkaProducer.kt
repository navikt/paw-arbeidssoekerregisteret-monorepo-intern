package no.nav.paw.arbeidssoekerregisteret.backup

import no.nav.paw.arbeidssoekerregisteret.backup.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.backup.utils.avsluttet
import no.nav.paw.arbeidssoekerregisteret.backup.utils.avvist
import no.nav.paw.arbeidssoekerregisteret.backup.utils.opplysninger
import no.nav.paw.arbeidssoekerregisteret.backup.utils.startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import kotlin.random.Random

private val logger = buildApplicationLogger

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    with(applicationConfig) {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        val pawHendelseKafkaProducer = kafkaFactory.createProducer<Long, Hendelse>(
            clientId = "$consumerGroupId-producer-${Random.nextInt()}",
            keySerializer = LongSerializer::class,
            valueSerializer = HendelseSerializer::class
        )

        val hendelser: Map<Long, Hendelse> = mapOf(
            Random.nextLong() to startet(),
            Random.nextLong() to avsluttet(),
            Random.nextLong() to avvist(),
            Random.nextLong() to opplysninger()
        )

        try {
            hendelser.forEach { (key, value) ->
                logger.info("Sender hendelse {}", value)
                pawHendelseKafkaProducer.send(ProducerRecord(hendelsesloggTopic, key, value)).get()
            }
        } catch (e: Exception) {
            logger.error("Send hendelse feilet", e)
        } finally {
            pawHendelseKafkaProducer.close()
        }
    }
}
