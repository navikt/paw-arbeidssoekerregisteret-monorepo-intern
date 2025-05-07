package no.nav.paw.kafkakeygenerator.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.config.KafkaConsumerConfig
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysIdentitetRepository
import no.nav.paw.logging.logger.buildNamedLogger
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class PdlAktorKafkaConsumerService(
    private val kafkaConsumerConfig: KafkaConsumerConfig,
    private val kafkaKeysIdentitetRepository: KafkaKeysIdentitetRepository,
    private val hwmOperations: KafkaHwmOperations,
    private val identitetService: IdentitetService
) {
    private val logger = buildNamedLogger("application.kafka")

    @WithSpan
    fun handleRecords(records: ConsumerRecords<String, Aktor>) {
        with(kafkaConsumerConfig) {
            if (records.isEmpty) {
                logger.trace("Ingen aktor-meldinger mottatt i poll-vindu fra {}", topic)
            } else {
                logger.info("Mottok {} aktor-meldinger fra {}", records.count(), topic)
                records.forEach(::handleRecord)
            }
        }
    }

    @WithSpan
    private fun handleRecord(record: ConsumerRecord<String, Aktor>) {
        transaction {
            with(kafkaConsumerConfig) {
                val rowsAffected = updateHwm(record)

                if (rowsAffected == 0) {
                    logger.info(
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
                    handleAktor(
                        aktorId = record.key(),
                        aktor = record.value(),
                        sourceTimestamp = Instant.ofEpochMilli(record.timestamp())
                    )
                }
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

            val eksisterendeIdentitetRows = identitetService
                .hentIdentiteterForAktorId(aktorId)

            if (eksisterendeIdentitetRows.isEmpty()) {
                logger.info("Ignorer tombstone-melding fordi ingen lagrede identiteter funnet")
            } else {
                identitetService.identiteterSkalSlettes(
                    aktorId = aktorId,
                    eksisterendeIdentitetRows = eksisterendeIdentitetRows
                )
            }
        } else {
            logger.info("Mottok melding om endring av identiteter")

            val identiteter = aktor.identifikatorer
                .map { it.idnummer }
                .toSet()
            val eksisterendeKafkaKeyRows = kafkaKeysIdentitetRepository
                .findByIdentiteter(identiteter)
            val arbeidssoekerIdSet = eksisterendeKafkaKeyRows
                .map { it.arbeidssoekerId }
                .toSet()

            if (arbeidssoekerIdSet.isEmpty()) {
                logger.info("Ignorer aktor-melding fordi person ikke er arbeidssøker")
            } else if (arbeidssoekerIdSet.size > 1) {
                logger.warn("Identiteter for aktor-melding har flere arbeidssøker-ider")
                identitetService.identiteterMedKonflikt(
                    aktorId = aktorId,
                    aktor = aktor,
                    sourceTimestamp = sourceTimestamp,
                    eksisterendeKafkaKeyRows = eksisterendeKafkaKeyRows
                )
            } else {
                val arbeidssoekerId = arbeidssoekerIdSet.first()
                identitetService.identiteterSkalEndres(
                    aktorId = aktorId,
                    aktor = aktor,
                    sourceTimestamp = sourceTimestamp,
                    arbeidssoekerId = arbeidssoekerId
                )
            }
        }
    }

    private fun updateHwm(record: ConsumerRecord<String, Aktor>): Int {
        with(kafkaConsumerConfig) {
            return hwmOperations.updateHwm(
                topic = topic,
                partition = record.partition(),
                offset = record.offset(),
                timestamp = Instant.ofEpochMilli(record.timestamp())
            )
        }
    }
}