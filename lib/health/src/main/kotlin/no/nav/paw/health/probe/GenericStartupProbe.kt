package no.nav.paw.health.probe

import no.nav.paw.health.model.MutableHealthCheck
import no.nav.paw.health.model.StartupCheck
import java.util.concurrent.atomic.AtomicBoolean

class GenericStartupProbe : StartupCheck, MutableHealthCheck {
    private val started = AtomicBoolean(false)
    override fun markHealthy() = started.set(true)
    override fun markUnhealthy() = started.set(false)
    override fun isStarted() = started.get()
}