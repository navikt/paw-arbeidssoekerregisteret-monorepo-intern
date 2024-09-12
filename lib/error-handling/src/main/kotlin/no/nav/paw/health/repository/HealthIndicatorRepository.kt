package no.nav.paw.health.repository

import no.nav.paw.health.model.HealthIndicatorList
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator

class HealthIndicatorRepository {

    private val readinessIndicators: HealthIndicatorList = mutableListOf()
    private val livenessIndicators: HealthIndicatorList = mutableListOf()

    fun addReadinessIndicator(healthIndicator: ReadinessHealthIndicator): ReadinessHealthIndicator {
        readinessIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun addLivenessIndicator(healthIndicator: LivenessHealthIndicator): LivenessHealthIndicator {
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