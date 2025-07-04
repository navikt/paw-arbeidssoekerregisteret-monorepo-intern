package no.nav.paw.arbeidssoekerregisteret.backup.health

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.kafka.consumer.NonCommittingKafkaConsumerWrapper
import no.nav.paw.logging.logger.buildApplicationLogger
import javax.sql.DataSource

private val logger = buildApplicationLogger

fun isDatabaseReady(dataSource: DataSource): Boolean = runCatching {
    dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT 1").execute()
    }
}.onFailure { error ->
    logger.error("Databasen er ikke klar enda", error)
}.getOrDefault(false)

fun isKafkaConsumerReady(consumerWrapper: NonCommittingKafkaConsumerWrapper<Long, Hendelse>): Boolean {
    return consumerWrapper.isRunning().also { isRunning ->
        if (!isRunning) {
            logger.error("Kafka-consumeren kjører ikke")
        }
    }
}