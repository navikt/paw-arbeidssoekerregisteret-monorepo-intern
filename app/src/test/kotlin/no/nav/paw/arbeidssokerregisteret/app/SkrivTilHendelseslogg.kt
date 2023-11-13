package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.config.KafkaKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Metadata
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
        valueSerializer = SpecificAvroSerde<SpecificRecord>().serializer()::class
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

    val producer = KafkaProducer<Long, SpecificRecord>(kafkaKonfigurasjon.properties + producerCfg)

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
        stop(periodeBruker2)
        stop(periodeBruker3)
        stop(periodeBruker1)
        start(periodeBruker2)
    }
    producer.flush()
    producer.close()
    Thread.sleep(5000)
    val events = periodeConsumer.poll(Duration.ofSeconds(1))
    periodeConsumer.commitSync()
    println("Antall eventer før start=${hendelserFørStart.count()}")
    println("Antall eventer=${events.count()}")
    assert(events.count() == 7)
    events.forEach { println(it.value()) }
}

class TestContext(private val producer: KafkaProducer<Long, SpecificRecord>, private val topic: String) {

    fun start(id: String) {
        producer.send(
            ProducerRecord(
                topic,
                id.hashCode().toLong(),
                Startet(
                    id,
                    Metadata(
                        Instant.now(),
                        Bruker(BrukerType.SLUTTBRUKER, "test"),
                        "unit-test",
                        "tester"
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
                Stoppet(
                    id,
                    Metadata(
                        Instant.now(),
                        Bruker(BrukerType.SYSTEM, "test"),
                        "unit-test",
                        "tester"
                    )
                )
            )
        )
            .get()
    }
}