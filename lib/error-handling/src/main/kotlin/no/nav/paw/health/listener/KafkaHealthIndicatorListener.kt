package no.nav.paw.health.listener

import no.nav.paw.health.model.HealthIndicator
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("paw.application.health.kafka")

fun KafkaStreams.withHealthIndicatorStateListener(
    livenessHealthIndicator: HealthIndicator,
    readinessHealthIndicator: HealthIndicator
) = KafkaStreams.StateListener { newState, previousState ->
    when (newState) {
        KafkaStreams.State.RUNNING -> {
            readinessHealthIndicator.setHealthy()
        }

        KafkaStreams.State.REBALANCING -> {
            readinessHealthIndicator.setHealthy()
        }

        KafkaStreams.State.PENDING_ERROR -> {
            readinessHealthIndicator.setUnhealthy()
        }

        KafkaStreams.State.PENDING_SHUTDOWN -> {
            readinessHealthIndicator.setUnhealthy()
        }

        KafkaStreams.State.ERROR -> {
            readinessHealthIndicator.setUnhealthy()
            livenessHealthIndicator.setUnhealthy()
        }

        else -> {
            readinessHealthIndicator.setUnknown()
        }
    }

    logger.debug("Kafka Streams state endret seg ${previousState.name} -> ${newState.name}")
    logger.info("Kafka Streams liveness er ${livenessHealthIndicator.getStatus().value}")
    logger.info("Kafka Streams readiness er ${readinessHealthIndicator.getStatus().value}")
}