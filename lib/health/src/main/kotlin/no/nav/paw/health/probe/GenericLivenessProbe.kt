package no.nav.paw.health.probe

import no.nav.paw.health.model.LivenessCheck
import no.nav.paw.health.model.MutableHealthCheck
import java.util.concurrent.atomic.AtomicBoolean

class GenericLivenessProbe(
    private val isAlive: AtomicBoolean = AtomicBoolean(false)
) : LivenessCheck, MutableHealthCheck {
    override fun markHealthy() = isAlive.set(true)
    override fun markUnhealthy() = isAlive.set(false)
    override fun isAlive(): Boolean = isAlive.get()
}