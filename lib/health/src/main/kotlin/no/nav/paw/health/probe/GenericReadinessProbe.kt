package no.nav.paw.health.probe

import no.nav.paw.health.model.MutableHealthCheck
import no.nav.paw.health.model.ReadinessCheck
import java.util.concurrent.atomic.AtomicBoolean

class GenericReadinessProbe(
    private val isReady: AtomicBoolean = AtomicBoolean(false)
) : ReadinessCheck, MutableHealthCheck {
    override fun markHealthy() = isReady.set(true)
    override fun markUnhealthy() = isReady.set(false)
    override fun isReady() = isReady.get()
}