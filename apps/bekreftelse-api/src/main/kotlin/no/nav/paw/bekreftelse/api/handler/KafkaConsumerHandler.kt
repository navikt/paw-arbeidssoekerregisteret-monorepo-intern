package no.nav.paw.bekreftelse.api.handler

import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.logging.logger.buildErrorLogger

class KafkaConsumerHandler(
    private val livenessIndicator: LivenessHealthIndicator,
    private val readinessIndicator: ReadinessHealthIndicator
) {
    private val errorLogger = buildErrorLogger

    fun handleException(throwable: Throwable) {
        errorLogger.error("Kafka Consumer opplevde en uhåndterbar feil", throwable)
        livenessIndicator.setUnhealthy()
        readinessIndicator.setUnhealthy()
        throw throwable
    }
}