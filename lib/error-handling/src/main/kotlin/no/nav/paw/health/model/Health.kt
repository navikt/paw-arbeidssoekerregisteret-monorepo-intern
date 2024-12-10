package no.nav.paw.health.model

import java.util.concurrent.atomic.AtomicReference

enum class HealthStatus(val value: String) {
    UNKNOWN("UNKNOWN"),
    HEALTHY("HEALTHY"),
    UNHEALTHY("UNHEALTHY"),
}

interface HealthIndicator {
    fun getStatus(): HealthStatus
}

interface MutableHealthIndicator : HealthIndicator {
    fun setUnknown();

    fun setHealthy();

    fun setUnhealthy();
}

open class DefaultHealthIndicator(initialStatus: HealthStatus) : MutableHealthIndicator {

    private val status = AtomicReference(initialStatus)

    override fun setUnknown() {
        status.set(HealthStatus.UNKNOWN)
    }

    override fun setHealthy() {
        status.set(HealthStatus.HEALTHY)
    }

    override fun setUnhealthy() {
        status.set(HealthStatus.UNHEALTHY)
    }

    override fun getStatus(): HealthStatus {
        return status.get()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultHealthIndicator) return false
        return status == other.status
    }

    override fun hashCode(): Int {
        return status.hashCode()
    }
}

typealias HealthIndicatorList = MutableList<HealthIndicator>

fun HealthIndicatorList.getAggregatedStatus(): HealthStatus {
    return if (this.isEmpty()) {
        HealthStatus.HEALTHY // Default til healthy
    } else if (this.all { it.getStatus() == HealthStatus.HEALTHY }) {
        HealthStatus.HEALTHY
    } else if (this.any { it.getStatus() == HealthStatus.UNHEALTHY }) {
        HealthStatus.UNHEALTHY
    } else {
        HealthStatus.UNKNOWN
    }
}

class ReadinessHealthIndicator(initialStatus: HealthStatus = HealthStatus.UNKNOWN) :
    DefaultHealthIndicator(initialStatus)

class LivenessHealthIndicator(initialStatus: HealthStatus = HealthStatus.HEALTHY) :
    DefaultHealthIndicator(initialStatus)