package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.producer

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KAFKA_TOPICS_CONFIG
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KafkaTopologyConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.test.TestData
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.serialization.kafka.JacksonSerializer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.StringSerializer

class PeriodeAvroSerializer : SpecificAvroSerializer<Periode>()
class VarselHendelseSerializer : JacksonSerializer<VarselHendelse>()

fun main() {
    val kafkaTopicsConfig = loadNaisOrLocalConfiguration<KafkaTopologyConfig>(KAFKA_TOPICS_CONFIG)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val kafkaFactory = KafkaFactory(kafkaConfig)

    val periodeKafkaProducer = kafkaFactory.createProducer<Long, Periode>(
        clientId = "bekreftelse-min-side-varsler-test-kafka-producer",
        keySerializer = LongSerializer::class,
        valueSerializer = PeriodeAvroSerializer::class
    )
    val bekreftelseKafkaProducer = kafkaFactory.createProducer<Long, BekreftelseHendelse>(
        clientId = "bekreftelse-min-side-varsler-test-kafka-producer",
        keySerializer = LongSerializer::class,
        valueSerializer = BekreftelseHendelseSerializer::class
    )
    val varselKafkaProducer = kafkaFactory.createProducer<String, VarselHendelse>(
        clientId = "bekreftelse-min-side-varsler-test-kafka-producer",
        keySerializer = StringSerializer::class,
        valueSerializer = VarselHendelseSerializer::class
    )

    //periodeKafkaProducer.sendPeriode(kafkaTopicsConfig.periodeTopic)
    //bekreftelseKafkaProducer.sendBekreftelseTilgjengelig(kafkaTopicsConfig.bekreftelseHendelseTopic)
    //bekreftelseKafkaProducer.sendBekreftelseMeldingMottatt(kafkaTopicsConfig.bekreftelseHendelseTopic)
    bekreftelseKafkaProducer.sendPeriodeAvsluttet(kafkaTopicsConfig.bekreftelseHendelseTopic)
    //varselKafkaProducer.sendVarselHendelse(kafkaTopicsConfig.tmsVarselHendelseTopic)
}

private fun Producer<Long, Periode>.sendPeriode(topic: String): RecordMetadata? {
    val periode = TestData.aapenPeriode1
    return send(ProducerRecord(topic, periode.key, periode.value)).get()
}

private fun Producer<Long, BekreftelseHendelse>.sendBekreftelseTilgjengelig(topic: String): RecordMetadata? {
    val bekreftelseHendelse = TestData.bekreftelseTilgjengelig1a
    return send(ProducerRecord(topic, bekreftelseHendelse.key, bekreftelseHendelse.value)).get()
}

private fun Producer<Long, BekreftelseHendelse>.sendBekreftelseMeldingMottatt(topic: String): RecordMetadata? {
    val bekreftelseHendelse = TestData.bekreftelseMeldingMottatt1
    return send(ProducerRecord(topic, bekreftelseHendelse.key, bekreftelseHendelse.value)).get()
}

private fun Producer<Long, BekreftelseHendelse>.sendPeriodeAvsluttet(topic: String): RecordMetadata? {
    val bekreftelseHendelse = TestData.periodeAvsluttet1
    return send(ProducerRecord(topic, bekreftelseHendelse.key, bekreftelseHendelse.value)).get()
}

private fun Producer<String, VarselHendelse>.sendVarselHendelse(topic: String): RecordMetadata? {
    val varselHendelse = TestData.oppgaveVarselHendelse1a
    return send(ProducerRecord(topic, varselHendelse.key, varselHendelse.value)).get()
}
