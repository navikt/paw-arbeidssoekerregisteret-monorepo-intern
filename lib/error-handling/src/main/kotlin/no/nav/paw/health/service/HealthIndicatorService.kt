package no.nav.paw.health.service

import no.nav.paw.health.model.HealthIndicator
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.StandardHealthIndicator

class HealthIndicatorService {

    private val readinessIndicators = mutableListOf<HealthIndicator>()
    private val livenessIndicators = mutableListOf<HealthIndicator>()

    fun addReadinessIndicator(): HealthIndicator {
        val healthIndicator = StandardHealthIndicator(HealthStatus.UNKNOWN)
        readinessIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun addLivenessIndicator(): HealthIndicator {
        val healthIndicator = StandardHealthIndicator(HealthStatus.HEALTHY)
        livenessIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun getReadinessStatus(): HealthStatus {
        return if (readinessIndicators.all { it.getStatus() == HealthStatus.HEALTHY }) {
            HealthStatus.HEALTHY
        } else if (readinessIndicators.any { it.getStatus() == HealthStatus.UNHEALTHY }) {
            HealthStatus.UNHEALTHY
        } else {
            HealthStatus.UNKNOWN
        }
    }

    fun getLivenessStatus(): HealthStatus {
        return if (livenessIndicators.all { it.getStatus() == HealthStatus.HEALTHY }) {
            HealthStatus.HEALTHY
        } else if (livenessIndicators.any { it.getStatus() == HealthStatus.UNHEALTHY }) {
            HealthStatus.UNHEALTHY
        } else {
            HealthStatus.UNKNOWN
        }
    }
}