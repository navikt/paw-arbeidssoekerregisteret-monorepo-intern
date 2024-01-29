package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.STILLING_STYRK08
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.config.KafkaKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import java.time.Duration
import java.time.Instant
import java.util.*

fun main() {
    val kafkaKonfigurasjon = lastKonfigurasjon<KafkaKonfigurasjon>(kafkaKonfigurasjonsfil)
    val producerCfg = kafkaProducerProperties(
        producerId = "test",
        keySerializer = Serdes.Long().serializer()::class,
        valueSerializer = HendelseSerde().serializer()::class
    )

    val periodeConsumer = KafkaConsumer<Long, Periode>(
        kafkaKonfigurasjon.properties +
                ("key.deserializer" to Serdes.Long().deserializer()::class.java.name) +
                ("value.deserializer" to SpecificAvroSerde<Periode>().deserializer()::class.java.name) +
                ("group.id" to "test")
    )

    periodeConsumer.subscribe(listOf(kafkaKonfigurasjon.streamKonfigurasjon.periodeTopic))
    val hendelserFørStart = periodeConsumer.poll(Duration.ofSeconds(1))
    periodeConsumer.commitSync()

    val producer = KafkaProducer<Long, Hendelse>(kafkaKonfigurasjon.properties + producerCfg)

    val periodeBruker1 = UUID.randomUUID().toString()
    val periodeBruker2 = UUID.randomUUID().toString()
    val periodeBruker3 = UUID.randomUUID().toString()
    with(TestContext(producer, kafkaKonfigurasjon.streamKonfigurasjon.eventlogTopic)) {
        start(periodeBruker1)
        start(periodeBruker1)
        start(periodeBruker1)
        start(periodeBruker3)
        start(periodeBruker1)
        start(periodeBruker2)
        opplysningerMottattPermitertOgDeltidsJobb(periodeBruker2)
        println("Sover i 30 sekunder")
        Thread.sleep(Duration.ofSeconds(30))
        println("Våkner")
        opplysningerMottattPermitert(periodeBruker2)
        stop(periodeBruker2)
        stop(periodeBruker3)
        stop(periodeBruker1)
        start(periodeBruker2)
    }
    producer.flush()
    producer.close()
    println("Hendelser før start=${hendelserFørStart.count()}")
    Thread.sleep(15000)
    val events = periodeConsumer.poll(Duration.ofSeconds(1))
    periodeConsumer.commitSync()
    println("Hendelser=${events.count()}")
    require(events.count() == 7) { "Forventet 7 hendelser, faktisk antall ${events.count()}"}
    events.forEach { println(it.value()) }
}

class TestContext(private val producer: KafkaProducer<Long, Hendelse>, private val topic: String) {

    fun start(id: String) {
        producer.send(
            ProducerRecord(
                topic,
                id.hashCode().toLong(),
                Startet(
                    hendelseId = UUID.randomUUID(),
                    identitetsnummer = id,
                    metadata = Metadata(
                        tidspunkt = Instant.now(),
                        utfoertAv = Bruker(BrukerType.SLUTTBRUKER, "test"),
                        kilde = "unit-test",
                        aarsak = "tester"
                    )
                )
            )
        )
            .get()
    }

    fun opplysningerMottattPermitertOgDeltidsJobb(id: String) {
        producer.send(
            ProducerRecord(
                topic,
                id.hashCode().toLong(),
                OpplysningerOmArbeidssoekerMottatt(
                    hendelseId = UUID.randomUUID(),
                    identitetsnummer = id,
                    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
                        metadata = Metadata(
                            tidspunkt = Instant.now(),
                            utfoertAv = Bruker(BrukerType.SLUTTBRUKER, "test"),
                            kilde = "unit-test",
                            aarsak = "tester"
                        ),
                        jobbsituasjon = Jobbsituasjon(
                            beskrivelser = listOf(
                                JobbsituasjonMedDetaljer(
                                    beskrivelse = JobbsituasjonBeskrivelse.ER_PERMITTERT,
                                    detaljer = mapOf(
                                        PROSENT to "100",
                                        STILLING_STYRK08 to "123"
                                    )
                                ),
                                JobbsituasjonMedDetaljer(
                                    beskrivelse = JobbsituasjonBeskrivelse.DELTIDSJOBB_VIL_MER,
                                    detaljer = mapOf(
                                        "prosent" to "25",
                                        "stillingStyrk08" to "124"
                                    )
                                )
                            )
                        ),
                        annet = Annet(JaNeiVetIkke.NEI),
                        utdanning = Utdanning(
                            nus = 7,
                            bestaatt = JaNeiVetIkke.JA,
                            godkjent = JaNeiVetIkke.JA
                        ),
                        helse = Helse(
                            helsetilstandHindrerArbeid = JaNeiVetIkke.NEI
                        ),
                        arbeidserfaring = Arbeidserfaring(harHattArbeid = JaNeiVetIkke.JA),
                        id = UUID.randomUUID()
                    )
                )
            )
        )
            .get()
    }

    fun opplysningerMottattPermitert(id: String) {
        producer.send(
            ProducerRecord(
                topic,
                id.hashCode().toLong(),
                OpplysningerOmArbeidssoekerMottatt(
                    hendelseId = UUID.randomUUID(),
                    identitetsnummer = id,
                    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
                        metadata = Metadata(
                            tidspunkt = Instant.now(),
                            utfoertAv = Bruker(BrukerType.SLUTTBRUKER, "test"),
                            kilde = "unit-test",
                            aarsak = "tester"
                        ),
                        jobbsituasjon = Jobbsituasjon(
                            beskrivelser = listOf(
                                JobbsituasjonMedDetaljer(
                                    beskrivelse = JobbsituasjonBeskrivelse.ER_PERMITTERT,
                                    detaljer = mapOf(
                                        PROSENT to "100",
                                        STILLING_STYRK08 to "123"
                                    )
                                )
                            )
                        ),
                        annet = Annet(JaNeiVetIkke.NEI),
                        utdanning = Utdanning(
                            nus = 7,
                            bestaatt = JaNeiVetIkke.JA,
                            godkjent = JaNeiVetIkke.JA
                        ),
                        helse = Helse(
                            helsetilstandHindrerArbeid = JaNeiVetIkke.NEI
                        ),
                        arbeidserfaring = Arbeidserfaring(harHattArbeid = JaNeiVetIkke.JA),
                        id = UUID.randomUUID()
                    )
                )
            )
        )
            .get()
    }

    fun stop(id: String) {
        producer.send(
            ProducerRecord(
                topic,
                id.hashCode().toLong(),
                Avsluttet(
                    hendelseId = UUID.randomUUID(),
                    identitetsnummer = id,
                    metadata = Metadata(
                        tidspunkt = Instant.now(),
                        utfoertAv = Bruker(BrukerType.SYSTEM, "test"),
                        kilde = "unit-test",
                        aarsak = "tester"
                    )
                )
            )
        )
            .get()
    }
}
