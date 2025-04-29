package no.nav.paw.kafka.handler

interface ConsumerExceptionHandler {
    fun handleException(throwable: Throwable)
}