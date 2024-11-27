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
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
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
    private val kafkaKeysRepository: KafkaKeysRepository,
    private val kafkaKeysAuditRepository: KafkaKeysAuditRepository,
) {
    private val logger = buildLogger
    private val errorLogger = buildErrorLogger
    private val livenessIndicator = healthIndicatorRepository
        .addLivenessIndicator(LivenessHealthIndicator(HealthStatus.HEALTHY))
    private val readinessIndicator = healthIndicatorRepository
        .addReadinessIndicator(ReadinessHealthIndicator(HealthStatus.HEALTHY))

    @WithSpan
    fun handleRecords(
        records: ConsumerRecords<Long, Hendelse>
    ) {
        records
            .onEach { record ->
                logger.debug(
                    "Mottok melding på topic: {}, partition: {}, offset {}",
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
                val identitetsnummer = event.alleIdentitetsnummer
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

        transaction(database) {
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
            val audit = Audit(
                identitetsnummer = identitetsnummer,
                tidligereArbeidssoekerId = fraArbeidssoekerId,
                identitetStatus = IdentitetStatus.VERIFISERT,
                detaljer = "Ingen endringer"
            )
            kafkaKeysAuditRepository.insert(audit)
        } else {
            // TODO: Skal vi kjøre noe om fraArbeidssoekerId != eksisterendeArbeidssoekerId

            logger.info("Identitetsnummer oppdateres med annen ArbeidsøkerId")
            meterRegistry.countKafkaUpdated()
            val count = identitetRepository.update(identitetsnummer, tilArbeidssoekerId)
            if (count != 0) {
                val audit = Audit(
                    identitetsnummer = identitetsnummer,
                    tidligereArbeidssoekerId = eksisterendeArbeidssoekerId,
                    identitetStatus = IdentitetStatus.OPPDATERT,
                    detaljer = "Bytte av arbeidsøkerId fra ${eksisterendeArbeidssoekerId.value} til ${tilArbeidssoekerId.value}"
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
                tidligereArbeidssoekerId = tilArbeidssoekerId,
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