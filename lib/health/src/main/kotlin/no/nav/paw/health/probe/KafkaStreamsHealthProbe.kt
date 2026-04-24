package no.nav.paw.health.probe

import no.nav.paw.health.model.LivenessCheck
import no.nav.paw.health.model.ReadinessCheck
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory

class KafkaStreamsHealthProbe(private val kafkaStreams: KafkaStreams) : LivenessCheck, ReadinessCheck {
    private val logger = LoggerFactory.getLogger("no.nav.paw.logger.health.kafka")

    override fun isAlive() = when (val state = kafkaStreams.state()) {
        KafkaStreams.State.CREATED -> true
        KafkaStreams.State.RUNNING -> true
        KafkaStreams.State.REBALANCING -> true
        else -> {
            logger.warn("Kafka Streams er ikke klar: {}", state.name)
            false
        }
    }

    override fun isReady() = when (kafkaStreams.state()) {
        KafkaStreams.State.RUNNING -> true
        KafkaStreams.State.REBALANCING -> true
        else -> false
    }
}
