package no.nav.paw.kafkakeygenerator.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.config.KafkaConsumerConfig
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KafkaKeyRow
import no.nav.paw.kafkakeygenerator.model.asIdentitetType
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.NyttIdentitetRepository
import no.nav.paw.logging.logger.buildNamedLogger
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class PdlAktorKafkaConsumerService(
    private val kafkaConsumerConfig: KafkaConsumerConfig,
    private val hwmOperations: KafkaHwmOperations,
    private val identitetRepository: IdentitetRepository,
    private val nyttIdentitetRepository: NyttIdentitetRepository
) {
    private val logger = buildNamedLogger("application.kafka")

    @WithSpan
    fun handleRecords(records: ConsumerRecords<String, Aktor>) {
        with(kafkaConsumerConfig) {
            if (records.isEmpty) {
                logger.trace("Ingen aktor-meldinger mottatt i poll-vindu fra {}", topic)
            } else {
                logger.info("Mottok {} aktor-meldinger fra {}", records.count(), topic)
                transaction {
                    records.forEach { record ->
                        val rowsUpdated = updateHwm(record)

                        if (rowsUpdated == 0) {
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
                            handleRecord(record)
                        }
                    }
                }
            }
        }
    }

    private fun handleRecord(record: ConsumerRecord<String, Aktor>) {
        val aktorId = record.key()
        val aktor = record.value()
        if (aktor == null) {
            val rowsAffected = nyttIdentitetRepository.updateStatusByAktorId(aktorId, IdentitetStatus.DELETED)
            // TODO: Slette hardt og brutalt?
            logger.info("Mottok tombstone-melding, soft-slettet {} tilhørende identiteter", rowsAffected)
        } else {
            logger.info("Mottok melding om endring i identiteter")
            val kafkaKeyList = identitetRepository
                .findByIdentiteter(aktor.identifikatorer.map { it.idnummer })

            if (kafkaKeyList.isEmpty()) {
                logger.info("Ignorer aktor-melding fordi person ikke er arbeidssøker")
            } else if (kafkaKeyList.size > 1) {
                handleIdentitetEndringMedKonflikt(record, kafkaKeyList)
            } else {
                val kafkaKey = kafkaKeyList.first()
                handleIdentitetEndring(record, kafkaKey.arbeidssoekerId)
            }
        }
    }

    private fun handleIdentitetEndringMedKonflikt(
        record: ConsumerRecord<String, Aktor>,
        kafkaKeyList: List<KafkaKeyRow>
    ) {
        val aktorId = record.key()
        val aktor = record.value()

        val eksisterendeKafkaKeys = kafkaKeyList
            .associateBy { it.identitetsnummer }
        val eksisterendeIdentiteter = nyttIdentitetRepository
            .findByAktorId(aktorId)
            .associateBy { it.identitet }

        aktor.identifikatorer.map { identifikator ->
            val eksisterendeIdentitet = eksisterendeIdentiteter[identifikator.idnummer]
            if (eksisterendeIdentitet == null) {
            } else {
            }
        }
        // TODO: Sette alle identiteter til CONFLICT
    }

    private fun handleIdentitetEndring(
        record: ConsumerRecord<String, Aktor>,
        arbeidssoekerId: Long
    ) {
        val aktorId = record.key()
        val aktor = record.value()

        val eksisterendeIdentiteter = nyttIdentitetRepository
            .findByAktorId(aktorId)
            .associateBy { it.identitet }

        aktor.identifikatorer.map { identifikator ->
            val eksisterendeIdentitet = eksisterendeIdentiteter[identifikator.idnummer]
            if (eksisterendeIdentitet == null) {
                val rowsAffected = nyttIdentitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId,
                    identitet = identifikator.idnummer,
                    type = identifikator.type.asIdentitetType(),
                    gjeldende = identifikator.gjeldende,
                    status = IdentitetStatus.VERIFIED,
                    sourceTimestamp = Instant.ofEpochMilli(record.timestamp())
                )
                logger.info(
                    "Lagret identitet av type {} (rows affected {})",
                    identifikator.type.name,
                    rowsAffected
                )
            } else {
                val rowsAffected = nyttIdentitetRepository.updateByAktorId(
                    aktorId = aktorId,
                    gjeldende = identifikator.gjeldende,
                )
                logger.info(
                    "Oppdaterer identitet av type {} (rows affected {})",
                    identifikator.type.name,
                    rowsAffected
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