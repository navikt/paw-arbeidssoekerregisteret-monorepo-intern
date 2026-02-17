package no.nav.paw.bekreftelse.api.service

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.exception.DataIkkeFunnetForIdException
import no.nav.paw.bekreftelse.api.exception.DataTilhoererIkkeBrukerException
import no.nav.paw.bekreftelse.api.handler.KafkaProducerHandler
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.model.asBekreftelse
import no.nav.paw.bekreftelse.api.model.asBekreftelseBruker
import no.nav.paw.bekreftelse.api.model.asBekreftelseRow
import no.nav.paw.bekreftelse.api.model.asTilgjengeligBekreftelse
import no.nav.paw.bekreftelse.api.models.MottaBekreftelseRequest
import no.nav.paw.bekreftelse.api.repository.BekreftelseRepository
import no.nav.paw.bekreftelse.api.utils.deleteBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.api.utils.ignoreBekreftelseHendeleCounter
import no.nav.paw.bekreftelse.api.utils.insertBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.api.utils.receiveBekreftelseCounter
import no.nav.paw.bekreftelse.api.utils.receiveBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.api.utils.sendBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.api.utils.setCreateActionAttribute
import no.nav.paw.bekreftelse.api.utils.setDeleteActionAttribute
import no.nav.paw.bekreftelse.api.utils.setEventAttribute
import no.nav.paw.bekreftelse.api.utils.setIgnoreActionAttribute
import no.nav.paw.bekreftelse.api.utils.setUpdateActionAttribute
import no.nav.paw.bekreftelse.api.utils.updateBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.meldingMottattHendelseType
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.config.env.appImageOrDefaultForLocal
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.logging.logger.buildLogger
import no.nav.paw.security.authentication.model.Bruker
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class BekreftelseService(
    private val serverConfig: ServerConfig,
    private val applicationConfig: ApplicationConfig,
    private val meterRegistry: MeterRegistry,
    private val kafkaKeysClient: KafkaKeysClient,
    private val kafkaProducerHandler: KafkaProducerHandler,
    private val bekreftelseRepository: BekreftelseRepository,
) {
    private val logger = buildLogger

    @WithSpan(value = "finnTilgjengeligBekreftelser")
    suspend fun finnTilgjengeligBekreftelser(identitetsnummer: Identitetsnummer): TilgjengeligBekreftelserResponse {
        val kafkaKeysResponse = kafkaKeysClient.getIdAndKey(identitetsnummer.value)

        return transaction {
            logger.info("Henter tilgjengelige bekreftelser")
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
        val kafkaKeysResponse = kafkaKeysClient.getIdAndKey(identitetsnummer.value)

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
                kafkaProducerHandler.sendBekreftelse(key, message)
                meterRegistry.sendBekreftelseHendelseCounter(meldingMottattHendelseType)
            } else {
                throw DataIkkeFunnetForIdException("Fant ingen bekreftelse for gitt id")
            }
        }
    }

    @WithSpan(value = "processBekreftelseHendelser")
    fun processBekreftelseHendelser(records: ConsumerRecords<Long, BekreftelseHendelse>) {
        transaction {
            records.forEach(::processBekreftelseHendelse)
        }
    }

    @WithSpan(value = "processBekreftelseHendelse")
    fun processBekreftelseHendelse(record: ConsumerRecord<Long, BekreftelseHendelse>) {
        val hendelse = record.value()
        meterRegistry.receiveBekreftelseHendelseCounter(hendelse.hendelseType)
        logger.info(
            "Mottok hendelse av type {} på topic: {}, partition: {}, offset {}",
            hendelse.hendelseType,
            record.topic(),
            record.partition(),
            record.offset()
        )

        when (hendelse) {
            is BekreftelseTilgjengelig -> {
                processBekreftelseTilgjengelig(record.partition(), record.offset(), record.key(), hendelse)
            }

            is BekreftelseMeldingMottatt -> {
                processBekreftelseMeldingMottatt(hendelse)
            }

            is BekreftelsePaaVegneAvStartet -> {
                processBekreftelsePaaVegneAvStartet(hendelse)
            }

            is PeriodeAvsluttet -> {
                processPeriodeAvsluttet(hendelse)
            }

            else -> {
                processAnnenHendelse(hendelse)
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
        currentSpan.setEventAttribute(hendelse.hendelseType)

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
            currentSpan.setUpdateActionAttribute()
            meterRegistry.updateBekreftelseHendelseCounter(hendelse.hendelseType, rowsAffected)
            logger.debug(
                "Oppdaterte bekreftelse av type {} (rows affected {})",
                hendelse.hendelseType,
                rowsAffected
            )
        } else {
            val rowsAffected = bekreftelseRepository.insert(nyRow)
            currentSpan.setCreateActionAttribute()
            meterRegistry.insertBekreftelseHendelseCounter(hendelse.hendelseType, rowsAffected)
            logger.debug("Opprettet bekreftelse av type {} (rows affected {})", hendelse.hendelseType, rowsAffected)
        }
    }

    @WithSpan(value = "processBekreftelseMeldingMottatt", kind = SpanKind.INTERNAL)
    private fun processBekreftelseMeldingMottatt(hendelse: BekreftelseMeldingMottatt) {
        val currentSpan = Span.current()
        val rowsAffected = bekreftelseRepository.deleteByBekreftelseId(hendelse.bekreftelseId)
        currentSpan.setEventAttribute(hendelse.hendelseType)
        currentSpan.setDeleteActionAttribute()
        meterRegistry.deleteBekreftelseHendelseCounter(hendelse.hendelseType, rowsAffected)
        logger.debug("Slettet bekreftelse(r) av type {} (rows affected {})", hendelse.hendelseType, rowsAffected)
    }

    @WithSpan(value = "processBekreftelsePaaVegneAvStartet", kind = SpanKind.INTERNAL)
    private fun processBekreftelsePaaVegneAvStartet(hendelse: BekreftelsePaaVegneAvStartet) {
        val currentSpan = Span.current()
        val rowsAffected = bekreftelseRepository.deleteByPeriodeId(hendelse.periodeId)
        currentSpan.setEventAttribute(hendelse.hendelseType)
        currentSpan.setDeleteActionAttribute()
        meterRegistry.deleteBekreftelseHendelseCounter(hendelse.hendelseType, rowsAffected)
        logger.debug("Slettet bekreftelse(r) av type {} (rows affected {})", hendelse.hendelseType, rowsAffected)
    }

    @WithSpan(value = "processPeriodeAvsluttet", kind = SpanKind.INTERNAL)
    private fun processPeriodeAvsluttet(hendelse: PeriodeAvsluttet) {
        val currentSpan = Span.current()
        val rowsAffected = bekreftelseRepository.deleteByPeriodeId(hendelse.periodeId)
        currentSpan.setEventAttribute(hendelse.hendelseType)
        currentSpan.setDeleteActionAttribute()
        meterRegistry.deleteBekreftelseHendelseCounter(hendelse.hendelseType, rowsAffected)
        logger.debug("Slettet bekreftelse(r) av type {} (rows affected {})", hendelse.hendelseType, rowsAffected)
    }

    @WithSpan(value = "processAnnenHendelse", kind = SpanKind.INTERNAL)
    private fun processAnnenHendelse(hendelse: BekreftelseHendelse) {
        val currentSpan = Span.current()
        currentSpan.setEventAttribute(hendelse.hendelseType)
        currentSpan.setIgnoreActionAttribute()
        meterRegistry.ignoreBekreftelseHendeleCounter(hendelse.hendelseType)
        logger.debug("Ignorerer hendelse av type {}", hendelse.hendelseType)
    }
}