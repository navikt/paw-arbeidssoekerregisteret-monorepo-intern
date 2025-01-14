package no.nav.paw.arbeidssoekerregisteret.backup

import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import java.time.Instant

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
    val producer: Producer<Long, Hendelse> = with(KafkaFactory(kafkaConfig)) {
        createProducer<Long, Hendelse>(
            clientId = "test-producer",
            keySerializer = LongSerializer::class,
            valueSerializer = HendelseSerializer::class,
            acks = "all"
        )
    }
    val startTime = Instant.now()
    val numberOfRecords = 12560
    hendelser().take(numberOfRecords).forEach {
        producer.send(ProducerRecord(
            "paw.arbeidssoker-hendelseslogg-v1",
            it.id,
            it
        ))
    }
    producer.flush()
    producer.close()
    println("Sendte $numberOfRecords hendelser på ${Instant.now().toEpochMilli() - startTime.toEpochMilli()} ms")
}

