package no.nav.paw.health.model

fun interface ReadinessCheck {
    fun isReady(): Boolean
}