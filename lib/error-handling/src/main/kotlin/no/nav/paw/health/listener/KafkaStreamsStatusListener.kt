package no.nav.paw.health.listener

import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.health.kafka")

fun KafkaStreams.withHealthIndicatorStateListener(
    livenessIndicator: LivenessHealthIndicator,
    readinessIndicator: ReadinessHealthIndicator
) {
    this.setStateListener(createHealthIndicatorStateListener(livenessIndicator, readinessIndicator))
}

fun createHealthIndicatorStateListener(
    livenessIndicator: LivenessHealthIndicator,
    readinessIndicator: ReadinessHealthIndicator
) = KafkaStreams.StateListener { newState, previousState ->
    when (newState) {
        KafkaStreams.State.CREATED -> {
            readinessIndicator.setUnhealthy()
            livenessIndicator.setHealthy()
        }

        KafkaStreams.State.RUNNING -> {
            readinessIndicator.setHealthy()
            livenessIndicator.setHealthy()
        }

        KafkaStreams.State.REBALANCING -> {
            readinessIndicator.setHealthy()
            livenessIndicator.setHealthy()
        }

        KafkaStreams.State.PENDING_ERROR -> {
            readinessIndicator.setUnhealthy()
            livenessIndicator.setHealthy()
        }

        KafkaStreams.State.ERROR -> {
            readinessIndicator.setUnhealthy()
            livenessIndicator.setUnhealthy()
        }

        KafkaStreams.State.PENDING_SHUTDOWN -> {
            readinessIndicator.setUnhealthy()
            livenessIndicator.setUnhealthy()
        }

        KafkaStreams.State.NOT_RUNNING -> {
            readinessIndicator.setUnhealthy()
            livenessIndicator.setUnhealthy()
        }

        else -> {
            readinessIndicator.setUnknown()
            livenessIndicator.setUnknown()
        }
    }

    logger.debug("Kafka Streams state endret seg ${previousState.name} -> ${newState.name}")
    logger.info("Kafka Streams liveness er ${livenessIndicator.getStatus().value}")
    logger.info("Kafka Streams readiness er ${readinessIndicator.getStatus().value}")
}