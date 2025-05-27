package no.nav.paw.health.repository

import no.nav.paw.health.model.HealthIndicator
import no.nav.paw.health.model.HealthIndicatorList
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.model.StartedHealthIndicator

class HealthIndicatorRepository {

    private val readinessIndicators: HealthIndicatorList = mutableListOf()
    private val livenessIndicators: HealthIndicatorList = mutableListOf()
    private val startupIndicators: HealthIndicatorList = mutableListOf()

    fun <T : HealthIndicator> addReadinessIndicator(healthIndicator: T): T {
        readinessIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun <T : HealthIndicator> addLivenessIndicator(healthIndicator: T): T {
        livenessIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun <T : HealthIndicator> addStartupIndicator(healthIndicator: T): T {
        startupIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun getReadinessIndicators(): HealthIndicatorList {
        return readinessIndicators
    }

    fun getLivenessIndicators(): HealthIndicatorList {
        return livenessIndicators
    }

    fun getStartupIndicators(): HealthIndicatorList {
        return livenessIndicators
    }

    fun readinessIndicator(defaultStatus: HealthStatus = HealthStatus.UNKNOWN): ReadinessHealthIndicator {
        val healthIndicator = ReadinessHealthIndicator(defaultStatus)
        readinessIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun livenessIndicator(defaultStatus: HealthStatus = HealthStatus.UNKNOWN): LivenessHealthIndicator {
        val healthIndicator = LivenessHealthIndicator(defaultStatus)
        livenessIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun startupIndicator(defaultStatus: HealthStatus = HealthStatus.UNKNOWN): StartedHealthIndicator {
        val healthIndicator = StartedHealthIndicator(defaultStatus)
        startupIndicators.add(healthIndicator)
        return healthIndicator
    }
}