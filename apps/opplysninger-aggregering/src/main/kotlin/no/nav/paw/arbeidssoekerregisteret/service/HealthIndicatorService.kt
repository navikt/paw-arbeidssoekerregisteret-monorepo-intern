package no.nav.paw.arbeidssoekerregisteret.service

import no.nav.paw.arbeidssoekerregisteret.config.HealthIndicator
import no.nav.paw.arbeidssoekerregisteret.config.StandardHealthIndicator
import no.nav.paw.arbeidssoekerregisteret.model.HealthStatus

class HealthIndicatorService {

    private val readinessIndicators = mutableListOf<HealthIndicator>()
    private val livenessIndicators = mutableListOf<HealthIndicator>()

    fun newReadinessIndicator(): HealthIndicator {
        val healthIndicator = StandardHealthIndicator(HealthStatus.UNKNOWN)
        readinessIndicators.add(healthIndicator)
        return healthIndicator
    }

    fun newLivenessIndicator(): HealthIndicator {
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