package no.nav.paw.kafkakeygenerator.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafkakeygenerator.config.KafkaConsumerConfig
import no.nav.paw.kafkakeygenerator.repository.PeriodeRepository
import no.nav.paw.logging.logger.buildErrorLogger
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class PawPeriodeKafkaConsumerService(
    private val kafkaConsumerConfig: KafkaConsumerConfig,
    private val hwmOperations: KafkaHwmOperations,
    private val periodeRepository: PeriodeRepository
) {

    private val logger = buildLogger
    private val errorLogger = buildErrorLogger

    @WithSpan
    fun handleRecords(records: ConsumerRecords<Long, Periode>) {
        with(kafkaConsumerConfig) {
            if (records.isEmpty) {
                logger.trace("Ingen periode-meldinger mottatt i poll-vindu fra {}", topic)
            } else {
                transaction {
                    logger.debug("Mottok {} periode-meldinger fra {}", records.count(), topic)
                    records.forEach(::handleRecord)
                }
            }
        }
    }

    @WithSpan
    private fun handleRecord(record: ConsumerRecord<Long, Periode>) {
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
                        "Ignorerer periode-melding på grunn av at offset {} på partition {} fra topic {} ikke er over HWM",
                        record.offset(),
                        record.partition(),
                        topic
                    )
                } else {
                    logger.info(
                        "Håndterer periode-melding med offset {} på partition {} fra topic {}",
                        record.offset(),
                        record.partition(),
                        topic
                    )
                    handlePeriode(
                        periode = record.value(),
                        sourceTimestamp = Instant.ofEpochMilli(record.timestamp())
                    )
                }
            } catch (e: Exception) {
                logger.error("Håndterer av periode-melding feilet", e)
                throw e
            }
        }
    }

    @WithSpan
    private fun handlePeriode(
        periode: Periode,
        sourceTimestamp: Instant
    ) {
        val periodeRow = periodeRepository.getByPeriodeId(periode.id)
        if (periodeRow == null) {
            val rowsAffected = periodeRepository.insert(
                periodeId = periode.id,
                identitet = periode.identitetsnummer,
                startetTimestamp = periode.startet.tidspunkt,
                avsluttetTimestamp = periode.avsluttet?.tidspunkt,
                sourceTimestamp = sourceTimestamp
            )
            logger.info("Lagret startet periode (rows affected {})", rowsAffected)
        } else {
            val rowsAffected = periodeRepository.updateAvsluttetTimestamp(
                periodeId = periode.id,
                avsluttetTimestamp = periode.avsluttet?.tidspunkt,
                sourceTimestamp = sourceTimestamp
            )
            logger.info("Lagret avsluttet periode (rows affected {})", rowsAffected)
        }
    }
}