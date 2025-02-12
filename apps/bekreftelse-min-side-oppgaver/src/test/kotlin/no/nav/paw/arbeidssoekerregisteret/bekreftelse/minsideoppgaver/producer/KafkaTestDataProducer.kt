package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.producer

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KAFKA_TOPICS_CONFIG
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KafkaTopicsConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.serialization.jackson.buildObjectMapper
import no.nav.paw.serialization.kafka.JacksonSerializer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Instant
import java.util.*

fun main() {

    val kafkaTopicsConfig = loadNaisOrLocalConfiguration<KafkaTopicsConfig>(KAFKA_TOPICS_CONFIG)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val kafkaFactory = KafkaFactory(kafkaConfig)

    sendPeriode(kafkaTopicsConfig, kafkaFactory)
    sendVarselHendelse(kafkaTopicsConfig, kafkaFactory)
}

class PeriodeAvroSerializer : SpecificAvroSerializer<Periode>()

private fun sendPeriode(
    kafkaTopicsConfig: KafkaTopicsConfig,
    kafkaFactory: KafkaFactory
) {
    val kafkaProducer = kafkaFactory.createProducer<Long, Periode>(
        clientId = "bekreftelse-min-side-varsler-test-kafka-producer",
        keySerializer = LongSerializer::class,
        valueSerializer = PeriodeAvroSerializer::class
    )

    val topic = kafkaTopicsConfig.periodeTopic
    val key = 1001L
    val value = Periode(
        UUID.randomUUID(),
        "01017012345",
        Metadata(Instant.now(), Bruker(BrukerType.SYSTEM, "test"), "test", "test", null),
        null
    )

    kafkaProducer.send(ProducerRecord(topic, key, value))
}

class VarselHendelseSerializer : JacksonSerializer<VarselHendelse>()

private fun sendVarselHendelse(
    kafkaTopicsConfig: KafkaTopicsConfig,
    kafkaFactory: KafkaFactory
) {
    val objectMapper = buildObjectMapper
    val kafkaProducer = kafkaFactory.createProducer<String, VarselHendelse>(
        clientId = "bekreftelse-min-side-varsler-test-kafka-producer",
        keySerializer = StringSerializer::class,
        valueSerializer = VarselHendelseSerializer::class
    )

    val topic = kafkaTopicsConfig.tmsVarselHendelseTopic
    val key = "01017012345"
    val value = VarselHendelse(
        eventName = VarselEventName.OPPRETTET,
        status = VarselStatus.VENTER,
        varselId = UUID.randomUUID().toString(),
        varseltype = VarselType.BESKJED,
        kanal = VarselKanal.SMS,
        renotifikasjon = null,
        sendtSomBatch = null,
        feilmelding = null,
        namespace = "paw",
        appnavn = "paw-arbeidssoekerregisteret-bekreftelse-min-side-varsler",
        tidspunkt = Instant.now()
    )
    kafkaProducer.send(ProducerRecord(topic, key, value).also {
        val json = objectMapper.writeValueAsString(it.value())
        println("AAAAAAAAAAAAAAAAAA: $json")
    })
}
