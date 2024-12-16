package no.nav.paw.health.listener

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.model.getAggregatedStatus
import no.nav.paw.health.repository.HealthIndicatorRepository
import org.apache.kafka.streams.KafkaStreams

class KafkaStreamsStatusListenerTest : FreeSpec({

    "Kafka Streams Status Listener skal returnere korrekt helsesjekk-status" {
        val healthIndicatorRepository = HealthIndicatorRepository()

        val liveness = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())
        val readiness = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())

        val listener = createHealthIndicatorStateListener(liveness, readiness)

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNKNOWN
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.HEALTHY

        listener.onChange(KafkaStreams.State.CREATED, KafkaStreams.State.CREATED)

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.HEALTHY

        listener.onChange(KafkaStreams.State.RUNNING, KafkaStreams.State.RUNNING)

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.HEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.HEALTHY

        listener.onChange(KafkaStreams.State.REBALANCING, KafkaStreams.State.REBALANCING)

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.HEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.HEALTHY

        listener.onChange(KafkaStreams.State.PENDING_ERROR, KafkaStreams.State.PENDING_ERROR)

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.HEALTHY

        listener.onChange(KafkaStreams.State.ERROR, KafkaStreams.State.ERROR)

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY

        listener.onChange(KafkaStreams.State.PENDING_SHUTDOWN, KafkaStreams.State.PENDING_SHUTDOWN)

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY

        listener.onChange(KafkaStreams.State.NOT_RUNNING, KafkaStreams.State.NOT_RUNNING)

        healthIndicatorRepository.getReadinessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY
        healthIndicatorRepository.getLivenessIndicators().getAggregatedStatus() shouldBe HealthStatus.UNHEALTHY
    }
})