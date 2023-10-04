package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.intern.StartV1
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

fun main() {
    val producerCfg = KafkaProducerProperties(
        producerId = "test",
        keySerializer = Serdes.String().serializer()::class,
        valueSerializer = SpecificAvroSerde<SpecificRecord>().serializer()::class
    )
    val cfgMap = producerCfg.map +
            (KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://localhost:8082") +
            (ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092") +
            (ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to Serdes.String().deserializer()::class.java.name) +
            (ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to SpecificAvroSerde<SpecificRecord>().deserializer()::class.java.name) +
            (ConsumerConfig.GROUP_ID_CONFIG to UUID.randomUUID().toString()) +
            (ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest")

    val consumer = KafkaConsumer<String, SpecificRecord>(cfgMap)
    consumer.subscribe(listOf("output"))
    val eventerFørStart = consumer.poll(Duration.ofSeconds(1))
    consumer.commitSync()

    val producer: KafkaProducer<String, SpecificRecord> = KafkaProducer(cfgMap)

    val id1 = UUID.randomUUID().toString()
    producer.send(ProducerRecord("input", id1, StartV1(id1, id1, System.currentTimeMillis())))
        .get(30, TimeUnit.SECONDS)
    producer.send(ProducerRecord("input", id1, StartV1(id1, id1, System.currentTimeMillis())))
        .get(30, TimeUnit.SECONDS)
    producer.flush()
    producer.close()

    val events = consumer.poll(Duration.ofSeconds(10))

    println("Antall eventer før start=${eventerFørStart.count()}")
    println("Antall eventer=${events.count()}")
    consumer.commitSync()
}