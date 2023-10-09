package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Start
import no.nav.paw.arbeidssokerregisteret.intern.v1.Stopp
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import java.time.Duration
import java.time.Instant
import java.util.*

fun main() {
    val producerCfg = KafkaProducerProperties(
        producerId = "test",
        keySerializer = Serdes.String().serializer()::class,
        valueSerializer = SpecificAvroSerde<SpecificRecord>().serializer()::class
    )
    val cfgMap = producerCfg.map +
            (KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://localhost:8082") +
            ("auto.register.schemas" to "true") +
            ("use.latest.version" to "true") +
            (ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092") +
            (ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to Serdes.String().deserializer()::class.java.name) +
            (ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to SpecificAvroSerde<SpecificRecord>().deserializer()::class.java.name) +
            (ConsumerConfig.GROUP_ID_CONFIG to UUID.randomUUID().toString()) +
            (ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest")

    val consumer = KafkaConsumer<String, SpecificRecord>(cfgMap)
    consumer.subscribe(listOf("periode-v1"))
    val eventerFørStart = consumer.poll(Duration.ofSeconds(1))
    consumer.commitSync()

    val producer: KafkaProducer<String, SpecificRecord> = KafkaProducer(cfgMap)

    val periodeBruker1 = UUID.randomUUID().toString()
    val periodeBruker2 = UUID.randomUUID().toString()
    val periodeBruker3 = UUID.randomUUID().toString()
    with(TestContext(producer, "input")) {
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
    Thread.sleep(15000)
    val events = consumer.poll(Duration.ofSeconds(10))
    consumer.commitSync()
    println("Antall eventer før start=${eventerFørStart.count()}")
    println("Antall eventer=${events.count()}")
    assert(events.count() == 7)
    events.forEach { println(it.value()) }
}

class TestContext(private val producer: KafkaProducer<String, SpecificRecord>, private val topic: String) {
    fun start(id: String) {
        producer.send(ProducerRecord(topic, id, Hendelse(UUID.randomUUID(), id, Instant.now(), "junit", "junit", Start())))
            .get()
    }

    fun stop(id: String) {
        producer.send(ProducerRecord(topic, id, Hendelse(UUID.randomUUID(), id, Instant.now(), "junit", "junit", Stopp("en test"))))
            .get()
    }
}