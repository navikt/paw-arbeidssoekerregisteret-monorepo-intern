package no.nav.paw.health.probe

import no.nav.paw.health.model.MutableHealthCheck
import no.nav.paw.health.model.StartupCheck
import java.util.concurrent.atomic.AtomicBoolean

class GenericStartupProbe(
    private val isStarted: AtomicBoolean = AtomicBoolean(false)
) : StartupCheck, MutableHealthCheck {
    override fun markHealthy() = isStarted.set(true)
    override fun markUnhealthy() = isStarted.set(false)
    override fun isStarted() = isStarted.get()
}