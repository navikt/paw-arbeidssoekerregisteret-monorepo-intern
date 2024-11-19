package no.nav.paw.kafkakeygenerator.service

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.utils.buildErrorLogger
import no.nav.paw.kafkakeygenerator.utils.buildLogger
import no.nav.paw.kafkakeygenerator.utils.countKafkaFailed
import no.nav.paw.kafkakeygenerator.utils.countKafkaIgnored
import no.nav.paw.kafkakeygenerator.utils.countKafkaInserted
import no.nav.paw.kafkakeygenerator.utils.countKafkaProcessed
import no.nav.paw.kafkakeygenerator.utils.countKafkaReceived
import no.nav.paw.kafkakeygenerator.utils.countKafkaUpdated
import no.nav.paw.kafkakeygenerator.utils.countKafkaVerified
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Audit
import no.nav.paw.kafkakeygenerator.vo.IdentitetStatus
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class KafkaConsumerService(
    private val database: Database,
    private val healthIndicatorRepository: HealthIndicatorRepository,
    private val meterRegistry: MeterRegistry,
    private val identitetRepository: IdentitetRepository,
    private val kafkaKeysAuditRepository: KafkaKeysAuditRepository,
) {
    private val logger = buildLogger
    private val errorLogger = buildErrorLogger
    private val livenessIndicator = healthIndicatorRepository
        .addLivenessIndicator(LivenessHealthIndicator(HealthStatus.HEALTHY))
    private val readinessIndicator = healthIndicatorRepository
        .addReadinessIndicator(ReadinessHealthIndicator(HealthStatus.HEALTHY))

    @WithSpan
    fun handleRecords(sequence: Sequence<ConsumerRecords<Long, Hendelse>>) {
        sequence.forEach { records ->
            records
                .map { it.value() }
                .onEach {
                    meterRegistry.countKafkaReceived()
                    if (it is IdentitetsnummerSammenslaatt) {
                        logger.debug("Prosesserer hendelse av type {}", it.hendelseType)
                        meterRegistry.countKafkaProcessed()
                    } else {
                        logger.debug("Ignorerer hendelse av type {}", it.hendelseType)
                        meterRegistry.countKafkaIgnored()
                    }
                }
                .filterIsInstance<IdentitetsnummerSammenslaatt>()
                .forEach { hendelse ->
                    logger.info("Mottok hendelse om sammenslåing av Identitetsnummer")
                    val identitetsnummer = hendelse.alleIdentitetsnummer
                        .map { Identitetsnummer(it) } + Identitetsnummer(hendelse.identitetsnummer)
                    val fraArbeidssoekerId = ArbeidssoekerId(hendelse.id)
                    val tilArbeidssoekerId = ArbeidssoekerId(hendelse.flyttetTilArbeidssoekerId)
                    updateIdentiteter(HashSet(identitetsnummer), fraArbeidssoekerId, tilArbeidssoekerId)
                }
        }
    }

    private fun updateIdentiteter(
        identitetsnummerSet: HashSet<Identitetsnummer>,
        fraArbeidssoekerId: ArbeidssoekerId,
        tilArbeidssoekerId: ArbeidssoekerId
    ) {
        transaction(database) {
            identitetRepository.find(fraArbeidssoekerId).let {
                if (it == null) {
                    meterRegistry.countKafkaFailed()
                    throw IllegalStateException("ArbeidssøkerId ikke funnet")
                }
            }
            identitetRepository.find(tilArbeidssoekerId).let {
                if (it == null) {
                    meterRegistry.countKafkaFailed()
                    throw IllegalStateException("ArbeidssøkerId ikke funnet")
                }
            }

            identitetsnummerSet.forEach { identitetsnummer ->
                val kafkaKey = identitetRepository.find(identitetsnummer)
                if (kafkaKey != null) {
                    updateIdentitet(identitetsnummer, fraArbeidssoekerId, tilArbeidssoekerId, kafkaKey.second)
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
            val audit = Audit(identitetsnummer, IdentitetStatus.VERIFISERT, "Ingen endringer")
            kafkaKeysAuditRepository.insert(audit)
        } else {
            logger.info("Identitetsnummer oppdateres med annen ArbeidsøkerId")
            meterRegistry.countKafkaUpdated()
            val count = identitetRepository.update(identitetsnummer, tilArbeidssoekerId)
            if (count != 0) {
                val audit = Audit(
                    identitetsnummer,
                    IdentitetStatus.OPPDATERT,
                    "Bytte av arbeidsøkerId fra ${fraArbeidssoekerId.value} til ${tilArbeidssoekerId.value}"
                )
                kafkaKeysAuditRepository.insert(audit)
            } else {
                logger.warn("Oppdatering førte ikke til noen endringer i databasen")
            }
        }
    }

    @WithSpan
    private fun insertIdentitet(
        identitetsnummer: Identitetsnummer,
        tilArbeidssoekerId: ArbeidssoekerId
    ) {
        logger.info("Identitetsnummer opprettes med eksisterende ArbeidsøkerId")
        meterRegistry.countKafkaInserted()
        val count = identitetRepository.insert(identitetsnummer, tilArbeidssoekerId)
        if (count != 0) {
            val audit = Audit(
                identitetsnummer = identitetsnummer,
                identitetStatus = IdentitetStatus.OPPRETTET,
                detaljer = "Opprettet ident for arbeidsøkerId ${tilArbeidssoekerId.value}"
            )
            kafkaKeysAuditRepository.insert(audit)
        } else {
            logger.warn("Opprettelse førte ikke til noen endringer i databasen")
        }
    }

    @WithSpan
    fun handleException(throwable: Throwable) {
        errorLogger.error("Kafka Consumer avslutter etter feil", throwable)
        Span.current().setStatus(StatusCode.ERROR)
        livenessIndicator.setUnhealthy()
        readinessIndicator.setUnhealthy()
    }
}