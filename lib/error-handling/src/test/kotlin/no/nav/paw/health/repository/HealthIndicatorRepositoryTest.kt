package no.nav.paw.health.repository

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.model.getAggregatedStatus

class HealthIndicatorRepositoryTest : FreeSpec({

    "Skal returnere korrekt helsesjekk-status" {
        val healthIndicatorRepository = HealthIndicatorRepository()

        val liveness1 = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())
        val liveness2 = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())
        val liveness3 = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())

        val readiness1 = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())
        val readiness2 = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())
        val readiness3 = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNKNOWN
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.HEALTHY

        readiness1.setUnhealthy()
        liveness1.setUnhealthy()

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY

        readiness2.setUnhealthy()
        readiness3.setUnhealthy()
        liveness2.setUnhealthy()
        liveness3.setUnhealthy()

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY

        readiness1.setHealthy()
        liveness1.setHealthy()

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY

        readiness2.setHealthy()
        liveness2.setHealthy()

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY

        readiness3.setHealthy()
        liveness3.setHealthy()

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.HEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.HEALTHY
    }
})