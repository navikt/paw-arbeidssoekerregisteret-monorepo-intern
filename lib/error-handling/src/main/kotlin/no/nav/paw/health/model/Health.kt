package no.nav.paw.health.model

import java.util.concurrent.atomic.AtomicReference

enum class HealthStatus(val value: String, val priority: Int) {
    UNKNOWN("UNKNOWN", 10),
    HEALTHY("HEALTHY", 5),
    UNHEALTHY("UNHEALTHY", 1),
}

open class HealthIndicator(initialStatus: HealthStatus) {

    private val status = AtomicReference(initialStatus)

    fun setUnknown() {
        status.set(HealthStatus.UNKNOWN)
    }

    fun setHealthy() {
        status.set(HealthStatus.HEALTHY)
    }

    fun setUnhealthy() {
        status.set(HealthStatus.UNHEALTHY)
    }

    fun getStatus(): HealthStatus {
        return status.get()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as HealthIndicator
        return status == other.status
    }

    override fun hashCode(): Int {
        return status.hashCode()
    }
}

typealias HealthIndicatorList = MutableList<HealthIndicator>

fun HealthIndicatorList.getAggregatedStatus(): HealthStatus =
    map(HealthIndicator::getStatus)
        .minByOrNull(HealthStatus::priority) ?: HealthStatus.UNKNOWN

class ReadinessHealthIndicator(initialStatus: HealthStatus = HealthStatus.UNKNOWN) : HealthIndicator(initialStatus)
class LivenessHealthIndicator(initialStatus: HealthStatus = HealthStatus.HEALTHY) : HealthIndicator(initialStatus)