package no.nav.paw.arbeidssoekerregisteret.producer

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
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

class PeriodeAvroSerializer : SpecificAvroSerializer<Periode>()
class VarselHendelseSerializer : JacksonSerializer<VarselHendelse>()

private fun <K, V> Producer<K, V>.send(topic: String, key: K, value: V) = send(ProducerRecord(topic, key, value)).get()
private fun <K, V> Producer<K, V>.send(topic: String, pair: Pair<K, V>) =
    send(topic, pair.first, pair.second)

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
        with(TestData) {

            val aapenPeriode = key1 to aapenPeriode(
                id = periodeId1,
                identitetsnummer = identitetsnummer1
            )
            val lukketPeriode = key1 to lukketPeriode(
                id = periodeId1,
                identitetsnummer = identitetsnummer1,
            )
            val bekreftelseTilgjengelig = key1 to bekreftelseTilgjengelig(
                periodeId = periodeId1,
                bekreftelseId = bekreftelseId1,
                arbeidssoekerId = arbeidssoekerId1
            )
            val bekreftelseMeldingMottatt = key1 to bekreftelseMeldingMottatt(
                periodeId = periodeId1,
                bekreftelseId = bekreftelseId1,
                arbeidssoekerId = arbeidssoekerId1
            )
            val periodeAvsluttet = key1 to periodeAvsluttet(
                periodeId = periodeId1,
                arbeidssoekerId = arbeidssoekerId1
            )
            val varselHendelse = bekreftelseId1.toString() to varselHendelse(
                eventName = VarselEventName.AKTIVERT,
                varselId = bekreftelseId1.toString(),
                varseltype = VarselType.OPPGAVE,
                status = VarselStatus.VENTER
            )

            periodeKafkaProducer.send(periodeTopic, aapenPeriode)
            //bekreftelseHendelseKafkaProducer.send(bekreftelseHendelseTopic, bekreftelseTilgjengelig)
            //bekreftelseHendelseKafkaProducer.send(bekreftelseHendelseTopic, bekreftelseMeldingMottatt)
            //bekreftelseHendelseKafkaProducer.send(bekreftelseHendelseTopic, periodeAvsluttet)
            //varselHendelseKafkaProducer.send(tmsVarselHendelseTopic, varselHendelse)
        }
    }
}
