package no.nav.paw.health.model

fun interface LivenessCheck {
    fun isAlive(): Boolean
}