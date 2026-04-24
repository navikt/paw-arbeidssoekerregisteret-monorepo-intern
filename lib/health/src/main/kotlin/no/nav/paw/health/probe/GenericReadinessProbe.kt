package no.nav.paw.health.probe

import no.nav.paw.health.model.MutableHealthCheck
import no.nav.paw.health.model.ReadinessCheck
import java.util.concurrent.atomic.AtomicBoolean

class GenericReadinessProbe : ReadinessCheck, MutableHealthCheck {
    private val ready = AtomicBoolean(false)
    override fun markHealthy() = ready.set(true)
    override fun markUnhealthy() = ready.set(false)
    override fun isReady() = ready.get()
}