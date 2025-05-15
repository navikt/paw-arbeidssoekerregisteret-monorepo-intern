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
    private val hwmOperations: KafkaHwmOperations,
    private val identitetService: IdentitetService,
    private val kafkaKeysIdentitetRepository: KafkaKeysIdentitetRepository
) {
    private val logger = buildNamedLogger("application.kafka")

    @WithSpan
    fun handleRecords(records: ConsumerRecords<Any, Aktor>) {
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
    private fun handleRecord(record: ConsumerRecord<Any, Aktor>) {
        transaction {
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
                        handleAktor(
                            aktorId = record.key().toString(),
                            aktor = record.value(),
                            sourceTimestamp = Instant.ofEpochMilli(record.timestamp())
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Håndterer av aktor-melding feilet", e)
                    throw e
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
            val identiteter = aktor.identifikatorer
                .map { it.idnummer }
                .toSet()
            val eksisterendeKafkaKeyRows = kafkaKeysIdentitetRepository
                .findByIdentiteter(identiteter)
            val arbeidssoekerIdSet = eksisterendeKafkaKeyRows
                .map { it.arbeidssoekerId }
                .toSet()

            if (arbeidssoekerIdSet.isEmpty()) {
                logger.info(
                    "Ignorer aktor-melding fordi person ikke er arbeidssøker ({} identiteter)",
                    identiteter.size
                )
            } else if (arbeidssoekerIdSet.size > 1) {
                logger.warn(
                    "Pauser aktor-melding fordi arbeidssøker har flere arbeidssøker-ider ({} identiteter)",
                    identiteter.size
                )
                identitetService.identiteterMedKonflikt(
                    aktorId = aktorId,
                    aktor = aktor,
                    sourceTimestamp = sourceTimestamp,
                    eksisterendeKafkaKeyRows = eksisterendeKafkaKeyRows
                )
            } else {
                logger.info("Endrer identiteter for arbeidssøker ({} identiteter)", identiteter.size)
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
}