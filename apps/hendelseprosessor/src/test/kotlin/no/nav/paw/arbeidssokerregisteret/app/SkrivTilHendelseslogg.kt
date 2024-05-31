package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.config.KafkaKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serdes
import java.time.Duration
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

    val periodeBruker1 = 100L to UUID.randomUUID().toString()
    val periodeBruker2 = 101L to UUID.randomUUID().toString()
    val periodeBruker3 = 102L to UUID.randomUUID().toString()
    with(TestContext(producer, kafkaKonfigurasjon.streamKonfigurasjon.eventlogTopic)) {
        start(periodeBruker1.first, periodeBruker1.second)
        start(periodeBruker1.first, periodeBruker1.second)
        start(periodeBruker1.first, periodeBruker1.second)
        start(periodeBruker3.first, periodeBruker3.second)
        start(periodeBruker1.first, periodeBruker1.second)
        start(periodeBruker2.first, periodeBruker2.second)
        opplysningerMottattPermitertOgDeltidsJobb(periodeBruker2.first, periodeBruker2.second)
        println("Sover i 30 sekunder")
        Thread.sleep(Duration.ofSeconds(30))
        println("Våkner")
        opplysningerMottattPermitert(periodeBruker2.first, periodeBruker2.second)
        stop(periodeBruker2.first, periodeBruker2.second)
        stop(periodeBruker3.first, periodeBruker3.second)
        stop(periodeBruker1.first, periodeBruker1.second)
        start(periodeBruker2.first, periodeBruker2.second)
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

