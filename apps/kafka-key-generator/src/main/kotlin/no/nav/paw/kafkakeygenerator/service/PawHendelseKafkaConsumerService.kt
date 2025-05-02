package no.nav.paw.kafkakeygenerator.service

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysIdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.utils.countKafkaFailed
import no.nav.paw.kafkakeygenerator.utils.countKafkaIgnored
import no.nav.paw.kafkakeygenerator.utils.countKafkaInserted
import no.nav.paw.kafkakeygenerator.utils.countKafkaProcessed
import no.nav.paw.kafkakeygenerator.utils.countKafkaReceived
import no.nav.paw.kafkakeygenerator.utils.countKafkaUpdated
import no.nav.paw.kafkakeygenerator.utils.countKafkaVerified
import no.nav.paw.kafkakeygenerator.utils.kafkaConflictGauge
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Audit
import no.nav.paw.kafkakeygenerator.vo.IdentitetStatus
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.logging.logger.buildErrorLogger
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.transactions.transaction

class PawHendelseKafkaConsumerService(
    private val meterRegistry: MeterRegistry,
    private val kafkaKeysIdentitetRepository: KafkaKeysIdentitetRepository,
    private val kafkaKeysRepository: KafkaKeysRepository,
    private val kafkaKeysAuditRepository: KafkaKeysAuditRepository,
) {
    private val logger = buildLogger
    private val errorLogger = buildErrorLogger

    @WithSpan
    fun handleRecords(records: ConsumerRecords<Long, Hendelse>) {
        records
            .onEach { record ->
                logger.info(
                    "Mottok hendelse av type {} på topic: {}, partition: {}, offset {}",
                    record.value().hendelseType,
                    record.topic(),
                    record.partition(),
                    record.offset()
                )
            }
            .map { it.value() }
            .onEach { event ->
                meterRegistry.countKafkaReceived()
                if (event is IdentitetsnummerSammenslaatt) {
                    logger.debug("Prosesserer hendelse av type {}", event.hendelseType)
                    meterRegistry.countKafkaProcessed()
                } else {
                    logger.debug("Ignorerer hendelse av type {}", event.hendelseType)
                    meterRegistry.countKafkaIgnored()
                }
            }
            .filterIsInstance<IdentitetsnummerSammenslaatt>()
            .forEach { event ->
                logger.info("Mottok hendelse om sammenslåing av Identitetsnummer")
                val identitetsnummer = event.flyttedeIdentitetsnumre
                    .map { Identitetsnummer(it) } + Identitetsnummer(event.identitetsnummer)
                val fraArbeidssoekerId = ArbeidssoekerId(event.id)
                val tilArbeidssoekerId = ArbeidssoekerId(event.flyttetTilArbeidssoekerId)
                updateIdentiteter(HashSet(identitetsnummer), fraArbeidssoekerId, tilArbeidssoekerId)
            }
    }

    private fun updateIdentiteter(
        identitetsnummerSet: HashSet<Identitetsnummer>,
        fraArbeidssoekerId: ArbeidssoekerId,
        tilArbeidssoekerId: ArbeidssoekerId
    ) {
        kafkaKeysRepository.find(fraArbeidssoekerId).let { arbeidssoekerId ->
            if (arbeidssoekerId == null) {
                meterRegistry.countKafkaFailed()
                throw IllegalStateException("ArbeidssøkerId ikke funnet")
            }
        }

        kafkaKeysRepository.find(tilArbeidssoekerId).let { arbeidssoekerId ->
            if (arbeidssoekerId == null) {
                meterRegistry.countKafkaFailed()
                throw IllegalStateException("ArbeidssøkerId ikke funnet")
            }
        }

        transaction {
            identitetsnummerSet.forEach { identitetsnummer ->
                val kafkaKey = kafkaKeysIdentitetRepository.find(identitetsnummer)
                if (kafkaKey != null) {
                    val eksisterendeArbeidssoekerId = ArbeidssoekerId(kafkaKey.arbeidssoekerId)
                    updateIdentitet(
                        identitetsnummer,
                        fraArbeidssoekerId,
                        tilArbeidssoekerId,
                        eksisterendeArbeidssoekerId
                    )
                } else {
                    insertIdentitet(identitetsnummer, tilArbeidssoekerId)
                }
            }
        }
    }

    @WithSpan
    private fun updateIdentitet(
        identitetsnummer: Identitetsnummer,
        fraArbeidssoekerId: ArbeidssoekerId,
        tilArbeidssoekerId: ArbeidssoekerId,
        eksisterendeArbeidssoekerId: ArbeidssoekerId
    ) {
        if (eksisterendeArbeidssoekerId == tilArbeidssoekerId) {
            logger.info("Identitetsnummer er allerede linket til korrekt ArbeidsøkerId")
            meterRegistry.countKafkaVerified()
            val audit = Audit(
                identitetsnummer = identitetsnummer,
                tidligereArbeidssoekerId = fraArbeidssoekerId,
                identitetStatus = IdentitetStatus.VERIFISERT,
                detaljer = "Ingen endringer"
            )
            kafkaKeysAuditRepository.insert(audit)
        } else if (eksisterendeArbeidssoekerId == fraArbeidssoekerId) {
            logger.info("Identitetsnummer oppdateres med annen ArbeidsøkerId")
            val count = kafkaKeysIdentitetRepository.update(identitetsnummer, tilArbeidssoekerId)
            if (count != 0) {
                meterRegistry.countKafkaUpdated()
                val audit = Audit(
                    identitetsnummer = identitetsnummer,
                    tidligereArbeidssoekerId = eksisterendeArbeidssoekerId,
                    identitetStatus = IdentitetStatus.OPPDATERT,
                    detaljer = "Bytte av arbeidsøkerId fra ${eksisterendeArbeidssoekerId.value} til ${tilArbeidssoekerId.value}"
                )
                kafkaKeysAuditRepository.insert(audit)
            } else {
                logger.warn("Oppdatering førte ikke til noen endringer i databasen")
                meterRegistry.countKafkaFailed()
                val audit = Audit(
                    identitetsnummer = identitetsnummer,
                    tidligereArbeidssoekerId = eksisterendeArbeidssoekerId,
                    identitetStatus = IdentitetStatus.IKKE_OPPDATERT,
                    detaljer = "Kunne ikke bytte arbeidsøkerId fra ${eksisterendeArbeidssoekerId.value} til ${tilArbeidssoekerId.value}"
                )
                kafkaKeysAuditRepository.insert(audit)
            }
        } else {
            logger.error("Eksisterende ArbeidssøkerId stemmer ikke med hendelse")
            meterRegistry.countKafkaFailed()
            val audit = Audit(
                identitetsnummer = identitetsnummer,
                tidligereArbeidssoekerId = fraArbeidssoekerId,
                identitetStatus = IdentitetStatus.KONFLIKT,
                detaljer = "Eksisterende arbeidsøkerId ${eksisterendeArbeidssoekerId.value} stemmer ikke med arbeidsøkerId fra hendelse ${fraArbeidssoekerId.value}"
            )
            kafkaKeysAuditRepository.insert(audit)
            val conflicts = kafkaKeysAuditRepository.findByStatus(IdentitetStatus.KONFLIKT)
            meterRegistry.kafkaConflictGauge(conflicts.size)
        }
    }

    @WithSpan
    private fun insertIdentitet(
        identitetsnummer: Identitetsnummer,
        tilArbeidssoekerId: ArbeidssoekerId
    ) {
        logger.info("Identitetsnummer opprettes med eksisterende ArbeidsøkerId")
        val count = kafkaKeysIdentitetRepository.insert(identitetsnummer, tilArbeidssoekerId)
        if (count != 0) {
            meterRegistry.countKafkaInserted()
            val audit = Audit(
                identitetsnummer = identitetsnummer,
                tidligereArbeidssoekerId = tilArbeidssoekerId,
                identitetStatus = IdentitetStatus.OPPRETTET,
                detaljer = "Opprettet ident for arbeidsøkerId ${tilArbeidssoekerId.value}"
            )
            kafkaKeysAuditRepository.insert(audit)
        } else {
            logger.warn("Opprettelse førte ikke til noen endringer i databasen")
            meterRegistry.countKafkaFailed()
            val audit = Audit(
                identitetsnummer = identitetsnummer,
                tidligereArbeidssoekerId = tilArbeidssoekerId,
                identitetStatus = IdentitetStatus.IKKE_OPPRETTET,
                detaljer = "Kunne ikke opprette ident for arbeidsøkerId ${tilArbeidssoekerId.value}"
            )
            kafkaKeysAuditRepository.insert(audit)
        }
    }
}