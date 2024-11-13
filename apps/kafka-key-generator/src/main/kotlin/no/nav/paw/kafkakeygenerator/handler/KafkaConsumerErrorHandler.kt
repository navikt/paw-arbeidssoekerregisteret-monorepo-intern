package no.nav.paw.kafkakeygenerator.handler

import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.utils.buildErrorLogger

class KafkaConsumerErrorHandler(
    healthIndicatorRepository: HealthIndicatorRepository
) {
    private val errorLogger = buildErrorLogger
    private val livenessIndicator = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())
    private val readinessIndicator = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())

    fun handleException(throwable: Throwable) {
        errorLogger.error("Kafka Consumer feilet", throwable)
        livenessIndicator.setUnhealthy()
        readinessIndicator.setUnhealthy()
    }
}