package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.arbeidssoekerregisteret.model.HealthStatus
import java.util.concurrent.atomic.AtomicReference

interface HealthIndicator {
    fun setUnknown()
    fun setHealthy()
    fun setUnhealthy()
    fun getStatus(): HealthStatus
}

class StandardHealthIndicator(initialStatus: HealthStatus) : HealthIndicator {

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
}