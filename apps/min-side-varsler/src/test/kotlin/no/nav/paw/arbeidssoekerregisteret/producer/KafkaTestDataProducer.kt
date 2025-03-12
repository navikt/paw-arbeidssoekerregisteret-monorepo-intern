package no.nav.paw.arbeidssoekerregisteret.producer

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.test.TestData
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
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class PeriodeAvroSerializer : SpecificAvroSerializer<Periode>()
class VarselHendelseSerializer : JacksonSerializer<VarselHendelse>()

private fun <K, V> Producer<K, V>.send(topic: String, key: K, value: V) = send(ProducerRecord(topic, key, value)).get()

fun main() {
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val kafkaFactory = KafkaFactory(kafkaConfig)

    val periodeKafkaProducer = kafkaFactory.createProducer<Long, Periode>(
        clientId = "bekreftelse-min-side-varsler-test-kafka-producer",
        keySerializer = LongSerializer::class,
        valueSerializer = PeriodeAvroSerializer::class
    )
    val bekreftelseHendelseKafkaProducer = kafkaFactory.createProducer<Long, BekreftelseHendelse>(
        clientId = "bekreftelse-min-side-varsler-test-kafka-producer",
        keySerializer = LongSerializer::class,
        valueSerializer = BekreftelseHendelseSerializer::class
    )
    val varselHendelseKafkaProducer = kafkaFactory.createProducer<String, VarselHendelse>(
        clientId = "bekreftelse-min-side-varsler-test-kafka-producer",
        keySerializer = StringSerializer::class,
        valueSerializer = VarselHendelseSerializer::class
    )

    with(applicationConfig) {
        val key1 = -10001L
        val key2 = -10002L
        val key3 = -10003L
        val key4 = -10004L
        val key5 = -10005L
        val identitetsnummer1 = "01017012345"
        val identitetsnummer2 = "02017012345"
        val identitetsnummer3 = "03017012345"
        val identitetsnummer4 = "04017012345"
        val identitetsnummer5 = "05017012345"
        val arbeidssoekerId1 = 10001L
        val arbeidssoekerId2 = 10002L
        val arbeidssoekerId3 = 10003L
        val arbeidssoekerId4 = 10004L
        val periodeId1 = UUID.fromString("4c0cb50a-3b4a-45df-b5b6-2cb45f04d19b")
        val periodeId2 = UUID.fromString("0fc3de47-a6cd-4ad5-8433-53235738200d")
        val periodeId3 = UUID.fromString("12cf8147-a76d-4b62-85d2-4792fea08995")
        val periodeId4 = UUID.fromString("f6f2f98a-2f2b-401d-b837-6ad26e45d4bf")
        val periodeId5 = UUID.fromString("f6384bc5-a0ec-4bdc-9262-f6ebf952269f")
        val bekreftelseId1 = UUID.fromString("0cd73e66-e5a2-4dae-88de-2dd89a910a19")
        val bekreftelseId2 = UUID.fromString("7b769364-4d48-40f8-ac64-4489bb8080dd")
        val bekreftelseId3 = UUID.fromString("b6e3b543-da44-4524-860f-9474bd6d505e")
        val bekreftelseId4 = UUID.fromString("a59581e6-c9be-4aec-b9f4-c635f1826c71")
        val bekreftelseId5 = UUID.fromString("de94b7ab-360f-4e5f-9ad1-3dc572b3e6a5")

        val aapenPeriode = TestData.aapenPeriode(
            id = periodeId1,
            identitetsnummer = identitetsnummer1
        )
        val lukketPeriode = TestData.lukketPeriode(
            id = periodeId1,
            identitetsnummer = identitetsnummer1,
        )
        val bekreftelseTilgjengelig = TestData.bekreftelseTilgjengelig(
            periodeId = periodeId2,
            bekreftelseId = bekreftelseId2,
            arbeidssoekerId = arbeidssoekerId2
        )
        val bekreftelseMeldingMottatt = TestData.bekreftelseMeldingMottatt(
            periodeId = periodeId1,
            bekreftelseId = bekreftelseId1,
            arbeidssoekerId = arbeidssoekerId1
        )
        val periodeAvsluttet = TestData.periodeAvsluttet(
            periodeId = periodeId1,
            arbeidssoekerId = arbeidssoekerId1
        )
        val varselHendelse = TestData.varselHendelse(
            eventName = VarselEventName.OPPRETTET,
            varselId = bekreftelseId1.toString(),
            varseltype = VarselType.OPPGAVE
        )

        //periodeKafkaProducer.send(periodeTopic, key1, lukketPeriode)
        bekreftelseHendelseKafkaProducer.send(bekreftelseHendelseTopic, key1, bekreftelseTilgjengelig)
        //bekreftelseHendelseKafkaProducer.send(bekreftelseHendelseTopic, key1, bekreftelseMeldingMottatt)
        //bekreftelseHendelseKafkaProducer.send(bekreftelseHendelseTopic, key1, periodeAvsluttet)
        //varselHendelseKafkaProducer.send(tmsVarselHendelseTopic, varselHendelse.varselId, varselHendelse)
    }
}
