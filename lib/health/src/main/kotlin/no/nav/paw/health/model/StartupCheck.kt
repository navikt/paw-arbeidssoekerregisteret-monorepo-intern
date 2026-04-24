package no.nav.paw.health.model

fun interface StartupCheck {
    fun isStarted(): Boolean
}