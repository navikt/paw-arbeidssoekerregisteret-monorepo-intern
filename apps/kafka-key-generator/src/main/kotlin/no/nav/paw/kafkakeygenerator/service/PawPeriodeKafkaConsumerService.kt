package no.nav.paw.kafkakeygenerator.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafka.config.KafkaConsumerConfig
import no.nav.paw.kafka.service.KafkaHwmOperations
import no.nav.paw.kafkakeygenerator.model.dao.PerioderTable
import no.nav.paw.logging.logger.buildErrorLogger
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

class PawPeriodeKafkaConsumerService(
    private val kafkaConsumerConfig: KafkaConsumerConfig,
    private val kafkaHwmOperations: KafkaHwmOperations
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
                    records
                        .asSequence()
                        .forEach(::handleRecord)
                }
            }
        }
    }

    @WithSpan
    private fun handleRecord(record: ConsumerRecord<Long, Periode>) {
        with(kafkaConsumerConfig) {
            try {
                val rowsAffected = kafkaHwmOperations.updateHwm(
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
    ) = transaction {
        val periodeRow = PerioderTable.getByPeriodeId(periode.id)
        if (periodeRow == null) {
            val rowsAffected = PerioderTable.insert(
                periodeId = periode.id,
                identitet = periode.identitetsnummer,
                startetTimestamp = periode.startet.tidspunkt,
                avsluttetTimestamp = periode.avsluttet?.tidspunkt,
                sourceTimestamp = sourceTimestamp
            )
            logger.info("Oppretter periode (rows affected {})", rowsAffected)
        } else {
            val rowsAffected = PerioderTable.updateAvsluttetTimestamp(
                periodeId = periode.id,
                avsluttetTimestamp = periode.avsluttet?.tidspunkt,
                sourceTimestamp = sourceTimestamp
            )
            logger.info("Oppdaterer periode (rows affected {})", rowsAffected)
        }
    }
}