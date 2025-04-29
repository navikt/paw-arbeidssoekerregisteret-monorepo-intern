package no.nav.paw.kafkakeygenerator.handler

import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.logging.logger.buildErrorLogger

class HealthIndicatorConsumerExceptionHandler(
    private val livenessIndicator: LivenessHealthIndicator,
    private val readinessIndicator: ReadinessHealthIndicator
) : ConsumerExceptionHandler {
    private val errorLogger = buildErrorLogger

    override fun handleException(throwable: Throwable) {
        errorLogger.error("Kafka Consumer opplevde en uhåndterbar feil", throwable)
        livenessIndicator.setUnhealthy()
        readinessIndicator.setUnhealthy()
        throw throwable
    }
}