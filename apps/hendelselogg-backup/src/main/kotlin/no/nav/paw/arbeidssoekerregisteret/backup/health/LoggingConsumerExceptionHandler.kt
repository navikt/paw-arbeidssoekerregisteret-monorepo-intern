package no.nav.paw.arbeidssoekerregisteret.backup.health

import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.logging.logger.buildErrorLogger

class LoggingConsumerExceptionHandler() : ConsumerExceptionHandler {
    private val errorLogger = buildErrorLogger

    override fun handleException(throwable: Throwable) {
        errorLogger.error("Kafka Consumer opplevde en uhåndterbar feil", throwable)
        throw throwable
    }
}