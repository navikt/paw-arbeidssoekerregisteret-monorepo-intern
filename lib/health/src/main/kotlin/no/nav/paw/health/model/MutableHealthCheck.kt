package no.nav.paw.health.model

interface MutableHealthCheck {
    fun markHealthy()
    fun markUnhealthy()
}