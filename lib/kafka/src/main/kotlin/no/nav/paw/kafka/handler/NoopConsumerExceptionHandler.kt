package no.nav.paw.kafka.handler

class NoopConsumerExceptionHandler : ConsumerExceptionHandler {
    override fun handleException(throwable: Throwable) {
        // Ignore
    }
}