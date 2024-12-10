package no.nav.paw.health.repository

import no.nav.paw.health.model.HealthIndicator
import no.nav.paw.health.model.HealthIndicatorList

class HealthIndicatorRepository {

    private val readinessIndicators: HealthIndicatorList = mutableListOf()
    private val livenessIndicators: HealthIndicatorList = mutableListOf()

    fun <T : HealthIndicator> addReadinessIndicator(healthIndicator: T): T {
        readinessIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun <T : HealthIndicator> addLivenessIndicator(healthIndicator: T): T {
        livenessIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun getReadinessIndicators(): HealthIndicatorList {
        return readinessIndicators
    }

    fun getLivenessIndicators(): HealthIndicatorList {
        return livenessIndicators
    }
}