package no.nav.paw.bekreftelse.api

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.paw.bekreftelse.api.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.config.kafka.sendDeferred
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import java.time.Duration
import java.time.Instant
import java.util.*

fun main() {

    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val kafkaFactory = KafkaFactory(applicationConfig.kafkaClients)
    val kafkaProducer = kafkaFactory.createProducer<Long, BekreftelseHendelse>(
        clientId = "bekreftelse-api-test-kafka-producer",
        keySerializer = LongSerializer::class,
        valueSerializer = BekreftelseHendelseSerializer::class
    )

    val arbeidssoekerId = 1L
    val periodeId = UUID.fromString("3e415602-b7b6-47d4-bbd7-efdda468ca20")
    val bekreftelseId = UUID.randomUUID()

    val testData = TestDataGenerator()

    val topic = applicationConfig.kafkaTopology.bekreftelseHendelsesloggTopic
    val key = 1L
    val value = testData.nyBekreftelseTilgjengelig(
        hendelseId = UUID.randomUUID(),
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        bekreftelseId = bekreftelseId,
        gjelderFra = Instant.now(),
        gjelderTil = Instant.now().plus(Duration.ofDays(14)),
    )

    sendHendelse(kafkaProducer, topic, key, value)
}

fun sendHendelse(
    producer: Producer<Long, BekreftelseHendelse>,
    topic: String,
    key: Long,
    value: BekreftelseHendelse
) =
    runBlocking {
        launch {
            producer.sendDeferred(ProducerRecord(topic, key, value)).await()
        }
    }