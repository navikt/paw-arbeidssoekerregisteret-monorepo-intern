package no.nav.paw.arbeidssoekerregisteret.backup.health

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.clients.consumer.Consumer
import javax.sql.DataSource

private val logger = buildApplicationLogger

fun isDatabaseReady(dataSource: DataSource): Boolean = runCatching {
    dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT 1").execute()
    }
}.onSuccess {
    logger.info("Databasen er klar for oppstart")
}.onFailure { error ->
    logger.error("Databasen er ikke klar enda", error)
}.getOrDefault(false)

fun isKafkaConsumerReady(consumer: Consumer<Long, Hendelse>): Boolean {
    synchronized(consumer) {
        val kafkaKlarForOppstart = consumer.assignment().isNotEmpty()
        when {
            kafkaKlarForOppstart -> logger.info("Kafka er klar for oppstart. Assigned topics: ${consumer.assignment()}")
            else -> logger.warn("Kafka consumer er ikke klar for oppstart.")
        }
        return kafkaKlarForOppstart
    }
}