package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.config.KafkaKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serdes
import java.time.Duration

fun main() {
    val kafkaKonfigurasjon = loadNaisOrLocalConfiguration<KafkaKonfigurasjon>(kafkaKonfigurasjonsfil)
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

    val periodeBruker1 = 100L to "12345678901"
    val periodeBruker2 = 101L to "12345678902"
    val periodeBruker3 = 102L to "12345678903"
    with(TestContext(producer, kafkaKonfigurasjon.streamKonfigurasjon.eventlogTopic)) {
        start(periodeBruker1.first, periodeBruker1.second)
        println("Startet periode for bruker (id=${periodeBruker1.first}, identitetsnummer=${periodeBruker1.second})")
        println("Trykker enter for å avslutte perioden")
        readlnOrNull()
        stop(periodeBruker1.first, periodeBruker1.second)
    }
    producer.flush()
    producer.close()
    println("Hendelser før start=${hendelserFørStart.count()}")
    Thread.sleep(15000)
    val events = periodeConsumer.poll(Duration.ofSeconds(1))
    periodeConsumer.commitSync()
    println("Hendelser=${events.count()}")
    require(events.count() == 2) { "Forventet 2 hendelser, faktisk antall ${events.count()}"}
    events.forEach { println(it.value()) }
}

