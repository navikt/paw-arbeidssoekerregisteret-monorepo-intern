package no.nav.paw.kafkakeymaintenance

import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.config.kafka.asSequence
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.LongDeserializer
import java.time.Duration

fun KafkaFactory.initPeriodeConsumer(
    periodeTopic: String,
    applicationContext: ApplicationContext
): Sequence<Iterable<ConsumerRecord<Long, Periode>>> {
    val periodeConsumer = createConsumer(
        groupId = "kafka-key-maintenance-v${applicationContext.consumerVersion}",
        clientId = "kafka-key-maintenance-client-v${applicationContext.consumerVersion}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = PeriodeDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest"
    )
    periodeConsumer.subscribe(listOf(periodeTopic))
    return periodeConsumer.asSequence(
        stop = applicationContext.shutdownCalled,
        pollTimeout = Duration.ofMillis(500),
        closeTimeout = Duration.ofSeconds(1)
    )
}

class PeriodeDeserializer : SpecificAvroDeserializer<Periode>()