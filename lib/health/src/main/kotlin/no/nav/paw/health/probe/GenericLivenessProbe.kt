package no.nav.paw.health.probe

import no.nav.paw.health.model.LivenessCheck
import no.nav.paw.health.model.MutableHealthCheck
import java.util.concurrent.atomic.AtomicBoolean

class GenericLivenessProbe : LivenessCheck, MutableHealthCheck {
    private val alive = AtomicBoolean(false)
    override fun markHealthy() = alive.set(true)
    override fun markUnhealthy() = alive.set(false)
    override fun isAlive(): Boolean = alive.get()
}