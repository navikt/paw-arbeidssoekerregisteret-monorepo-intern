package no.nav.paw.kafkakeymaintenance

import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.config.kafka.asSequence
import no.nav.paw.kafkakeymaintenance.kafka.HwmRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.LongDeserializer
import java.time.Duration

fun KafkaFactory.initPeriodeConsumer(
    periodeTopic: String,
    applicationContext: ApplicationContext
): Pair<HwmRebalanceListener, Sequence<Iterable<ConsumerRecord<Long, Periode>>>> {
    val periodeConsumer = createConsumer(
        groupId = "kafka-key-maintenance-v${applicationContext.consumerVersion}",
        clientId = "kafka-key-maintenance-client-v${applicationContext.consumerVersion}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = PeriodeDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest",
        maxPollrecords = 1000
    )
    val reblancingListener = HwmRebalanceListener(applicationContext, periodeConsumer)
    periodeConsumer.subscribe(listOf(periodeTopic), reblancingListener)
    return reblancingListener to periodeConsumer.asSequence(
        stop = applicationContext.shutdownCalled,
        pollTimeout = Duration.ofMillis(1000),
        closeTimeout = Duration.ofSeconds(1)
    )
}

class PeriodeDeserializer : SpecificAvroDeserializer<Periode>()