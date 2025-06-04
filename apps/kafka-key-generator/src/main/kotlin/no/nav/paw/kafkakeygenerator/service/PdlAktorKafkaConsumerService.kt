package no.nav.paw.kafkakeygenerator.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.config.KafkaConsumerConfig
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.model.asPerson
import no.nav.paw.kafkakeygenerator.utils.SecureLogger
import no.nav.paw.logging.logger.buildNamedLogger
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class PdlAktorKafkaConsumerService(
    private val kafkaConsumerConfig: KafkaConsumerConfig,
    private val hwmOperations: KafkaHwmOperations,
    private val identitetService: IdentitetService
) {
    private val logger = buildNamedLogger("application.kafka")

    @WithSpan
    fun handleRecords(records: ConsumerRecords<Any, Aktor>) {
        with(kafkaConsumerConfig) {
            if (records.isEmpty) {
                logger.trace("Ingen aktor-meldinger mottatt i poll-vindu fra {}", topic)
            } else {
                logger.debug("Mottok {} aktor-meldinger fra {}", records.count(), topic)
                transaction {
                    records.forEach(::handleRecord)
                }
            }
        }
    }

    @WithSpan
    private fun handleRecord(record: ConsumerRecord<Any, Aktor>) {
        with(kafkaConsumerConfig) {
            try {
                val rowsAffected = hwmOperations.updateHwm(
                    topic = topic,
                    partition = record.partition(),
                    offset = record.offset(),
                    timestamp = Instant.ofEpochMilli(record.timestamp())
                )

                if (rowsAffected == 0) {
                    logger.warn(
                        "Ignorerer aktor-melding på grunn av at offset {} på partition {} fra topic {} ikke er over HWM",
                        record.offset(),
                        record.partition(),
                        topic
                    )
                } else {
                    logger.info(
                        "Håndterer aktor-melding med offset {} på partition {} fra topic {}",
                        record.offset(),
                        record.partition(),
                        topic
                    )
                    /* TODO handleAktor(
                        aktorId = record.key().toString(),
                        aktor = record.value(),
                        sourceTimestamp = Instant.ofEpochMilli(record.timestamp())
                    )*/

                    if (setOf("2002308243366", "2647237114816").contains(record.key())) {
                        SecureLogger.warn(
                            "Aktor partition: {} offset: {} timestamp: {} key: {} value: {}",
                            record.partition(),
                            record.offset(),
                            Instant.ofEpochMilli(record.timestamp()),
                            record.key(),
                            record.value()?.asPerson() ?: "null"
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Håndterer av aktor-melding feilet", e)
                SecureLogger.error(
                    "Håndterer av aktor-melding feilet key: {} value: {} timestamp: {}",
                    record.key(),
                    record.value()?.asPerson() ?: "null",
                    Instant.ofEpochMilli(record.timestamp()),
                )
                throw e
            }
        }
    }

    private fun handleAktor(
        aktorId: String,
        aktor: Aktor?,
        sourceTimestamp: Instant
    ) {
        if (aktor == null) {
            logger.info("Mottok melding om sletting av identiteter")
            identitetService.identiteterSkalSlettes(aktorId)
        } else {
            logger.info("Mottok melding om oppdatering av identiteter")
            val identiteter = aktor.identifikatorer
                .map { it.asIdentitet() }
            identitetService.identiteterSkalOppdateres(aktorId, identiteter, sourceTimestamp)
        }
    }
}