package no.nav.paw.bekreftelse.api.services

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.exception.DataIkkeFunnetForIdException
import no.nav.paw.bekreftelse.api.exception.DataTilhoererIkkeBrukerException
import no.nav.paw.bekreftelse.api.model.MottaBekreftelseRequest
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.model.asBekreftelse
import no.nav.paw.bekreftelse.api.model.asBekreftelseBruker
import no.nav.paw.bekreftelse.api.model.asBekreftelseRow
import no.nav.paw.bekreftelse.api.model.asTilgjengeligBekreftelse
import no.nav.paw.bekreftelse.api.producer.BekreftelseKafkaProducer
import no.nav.paw.bekreftelse.api.repository.BekreftelseRepository
import no.nav.paw.bekreftelse.api.utils.buildLogger
import no.nav.paw.bekreftelse.api.utils.deleteBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.api.utils.ignoreBekreftelseHendeleCounter
import no.nav.paw.bekreftelse.api.utils.insertBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.api.utils.receiveBekreftelseCounter
import no.nav.paw.bekreftelse.api.utils.receiveBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.api.utils.sendBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.api.utils.updateBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.meldingMottattHendelseType
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.config.env.appImageOrDefaultForLocal
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.model.Bruker
import no.nav.paw.security.authentication.model.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction

class BekreftelseService(
    private val serverConfig: ServerConfig,
    private val applicationConfig: ApplicationConfig,
    private val meterRegistry: MeterRegistry,
    private val kafkaKeysClient: KafkaKeysClient,
    private val bekreftelseKafkaProducer: BekreftelseKafkaProducer,
    private val bekreftelseRepository: BekreftelseRepository,
) {
    private val logger = buildLogger

    @WithSpan(value = "finnTilgjengeligBekreftelser")
    suspend fun finnTilgjengeligBekreftelser(identitetsnummer: Identitetsnummer): TilgjengeligBekreftelserResponse {
        val kafkaKeysResponse = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)

        return transaction {
            logger.info("Skal hente tilgjengelige bekreftelser")
            val bekreftelser = bekreftelseRepository.findByArbeidssoekerId(kafkaKeysResponse.id)
            bekreftelser.map { it.asTilgjengeligBekreftelse() }
        }
    }

    @WithSpan(value = "mottaBekreftelse")
    suspend fun mottaBekreftelse(
        bruker: Bruker<*>,
        identitetsnummer: Identitetsnummer,
        request: MottaBekreftelseRequest
    ) {
        val kafkaKeysResponse = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)

        return transaction {
            logger.info("Har mottatt bekreftelse")
            meterRegistry.receiveBekreftelseCounter(meldingMottattHendelseType)

            val kilde = serverConfig.runtimeEnvironment.appImageOrDefaultForLocal()

            val bekreftelse = bekreftelseRepository.getByBekreftelseId(request.bekreftelseId)
            if (bekreftelse != null) {

                if (bekreftelse.arbeidssoekerId != kafkaKeysResponse.id) {
                    throw DataTilhoererIkkeBrukerException("Bekreftelse tilhører ikke bruker")
                }
                val key = bekreftelse.recordKey
                val message = bekreftelse.data.asBekreftelse(
                    request.harJobbetIDennePerioden,
                    request.vilFortsetteSomArbeidssoeker,
                    bruker.asBekreftelseBruker(),
                    kilde,
                    "Bekreftelse levert",
                    Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET
                )

                logger.debug("Sletter bekreftelse fra database")
                bekreftelseRepository.deleteByBekreftelseId(bekreftelse.bekreftelseId)
                bekreftelseKafkaProducer.produceMessage(key, message)
                meterRegistry.sendBekreftelseHendelseCounter(meldingMottattHendelseType)
            } else {
                throw DataIkkeFunnetForIdException("Fant ingen bekreftelse for gitt id")
            }
        }
    }

    @WithSpan(value = "processBekreftelseHendelse")
    fun processBekreftelseHendelse(record: ConsumerRecord<Long, BekreftelseHendelse>) {
        transaction {
            val hendelse = record.value()

            meterRegistry.receiveBekreftelseHendelseCounter(hendelse.hendelseType)

            logger.debug("Mottok hendelse av type {}", hendelse.hendelseType)

            when (hendelse) {
                is BekreftelseTilgjengelig -> {
                    processBekreftelseTilgjengelig(record.partition(), record.offset(), record.key(), hendelse)
                }

                is BekreftelseMeldingMottatt -> {
                    processBekreftelseMeldingMottatt(hendelse)
                }

                is PeriodeAvsluttet -> {
                    processPeriodeAvsluttet(hendelse)
                }

                else -> {
                    processAnnenHendelse(hendelse)
                }
            }
        }
    }

    @WithSpan(value = "processBekreftelseTilgjengelig", kind = SpanKind.INTERNAL)
    private fun processBekreftelseTilgjengelig(
        partition: Int,
        offset: Long,
        key: Long,
        hendelse: BekreftelseTilgjengelig
    ) {
        val currentSpan = Span.current()
        currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)

        val eksisterendeRow = bekreftelseRepository.getByBekreftelseId(hendelse.bekreftelseId)
        val nyRow = hendelse.asBekreftelseRow(
            applicationConfig.kafkaTopology.version,
            partition,
            offset,
            key
        )

        if (eksisterendeRow != null) {
            if (!eksisterendeRow.harSammeOffset(nyRow)) {
                logger.warn("Ny bekreftelse er lik eksisterende men har forskjellig Kafka offset")
            }
            val rowsAffected = bekreftelseRepository.update(nyRow)
            currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "update")
            meterRegistry.updateBekreftelseHendelseCounter(hendelse.hendelseType, rowsAffected)
            logger.debug("Oppdaterte bekreftelse av type {} (rows affected {})", hendelse.hendelseType, rowsAffected)
        } else {
            val rowsAffected = bekreftelseRepository.insert(nyRow)
            currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "insert")
            meterRegistry.insertBekreftelseHendelseCounter(hendelse.hendelseType, rowsAffected)
            logger.debug("Opprettet bekreftelse av type {} (rows affected {})", hendelse.hendelseType, rowsAffected)
        }
    }

    @WithSpan(value = "processBekreftelseMeldingMottatt", kind = SpanKind.INTERNAL)
    private fun processBekreftelseMeldingMottatt(hendelse: BekreftelseMeldingMottatt) {
        val rowsAffected = bekreftelseRepository.deleteByBekreftelseId(hendelse.bekreftelseId)

        val currentSpan = Span.current()
        currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)
        currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "delete")
        meterRegistry.deleteBekreftelseHendelseCounter(hendelse.hendelseType, rowsAffected)
        logger.debug("Slettet bekreftelse(r) av type {} (rows affected {})", hendelse.hendelseType, rowsAffected)
    }

    @WithSpan(value = "processPeriodeAvsluttet", kind = SpanKind.INTERNAL)
    private fun processPeriodeAvsluttet(hendelse: PeriodeAvsluttet) {
        val rowsAffected = bekreftelseRepository.deleteByPeriodeId(hendelse.periodeId)

        val currentSpan = Span.current()
        currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)
        currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "delete")
        meterRegistry.deleteBekreftelseHendelseCounter(hendelse.hendelseType, rowsAffected)
        logger.debug("Slettet bekreftelse(r) av type {} (rows affected {})", hendelse.hendelseType, rowsAffected)
    }

    @WithSpan(value = "processAnnenHendelse", kind = SpanKind.INTERNAL)
    private fun processAnnenHendelse(hendelse: BekreftelseHendelse) {
        val currentSpan = Span.current()
        currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)
        currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "ignore")
        meterRegistry.ignoreBekreftelseHendeleCounter(hendelse.hendelseType)
        logger.debug("Ignorerer hendelse av type {}", hendelse.hendelseType)
    }
}