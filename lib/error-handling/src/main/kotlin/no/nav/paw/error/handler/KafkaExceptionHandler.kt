package no.nav.paw.error.handler

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("paw.application.error.kafka")

fun KafkaStreams.withApplicationTerminatingExceptionHandler() = StreamsUncaughtExceptionHandler { throwable ->
    logger.error("Kafka Streams opplevde en uventet feil", throwable)
    StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
}