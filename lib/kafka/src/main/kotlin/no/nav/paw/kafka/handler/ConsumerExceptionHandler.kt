package no.nav.paw.kafka.handler

fun interface ConsumerExceptionHandler {
    fun handleException(throwable: Throwable)
}