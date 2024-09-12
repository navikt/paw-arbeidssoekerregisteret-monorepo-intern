package no.nav.paw.health.model

import java.util.concurrent.atomic.AtomicReference

enum class HealthStatus(val value: String) {
    UNKNOWN("UNKNOWN"),
    HEALTHY("HEALTHY"),
    UNHEALTHY("UNHEALTHY"),
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

fun HealthIndicatorList.getAggregatedStatus(): HealthStatus {
    return if (this.isEmpty()) {
        HealthStatus.UNKNOWN
    } else if (this.all { it.getStatus() == HealthStatus.HEALTHY }) {
        HealthStatus.HEALTHY
    } else if (this.any { it.getStatus() == HealthStatus.UNHEALTHY }) {
        HealthStatus.UNHEALTHY
    } else {
        HealthStatus.UNKNOWN
    }
}

class ReadinessHealthIndicator(initialStatus: HealthStatus = HealthStatus.UNKNOWN) : HealthIndicator(initialStatus)
class LivenessHealthIndicator(initialStatus: HealthStatus = HealthStatus.HEALTHY) : HealthIndicator(initialStatus)