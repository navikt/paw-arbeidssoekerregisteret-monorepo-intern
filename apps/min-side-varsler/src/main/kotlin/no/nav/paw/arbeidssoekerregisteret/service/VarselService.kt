package no.nav.paw.arbeidssoekerregisteret.service

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.models.VarselResponse
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.exception.PeriodeIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.exception.VarselIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselMelding
import no.nav.paw.arbeidssoekerregisteret.model.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.model.asInsertEksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asInsertPeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.asInsertVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asResponse
import no.nav.paw.arbeidssoekerregisteret.model.asUpdateEksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asUpdatePeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.asUpdateVarselRow
import no.nav.paw.arbeidssoekerregisteret.repository.EksterntVarselRepository
import no.nav.paw.arbeidssoekerregisteret.repository.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.repository.VarselRepository
import no.nav.paw.arbeidssoekerregisteret.utils.Action
import no.nav.paw.arbeidssoekerregisteret.utils.action
import no.nav.paw.arbeidssoekerregisteret.utils.bekreftelseHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.eventName
import no.nav.paw.arbeidssoekerregisteret.utils.periodeCounter
import no.nav.paw.arbeidssoekerregisteret.utils.removeAll
import no.nav.paw.arbeidssoekerregisteret.utils.varselHendelseCounter
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.logging.logger.buildLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.MDC
import java.util.*

class VarselService(
    private val applicationConfig: ApplicationConfig,
    private val meterRegistry: MeterRegistry,
    private val periodeRepository: PeriodeRepository,
    private val varselRepository: VarselRepository,
    private val eksterntVarselRepository: EksterntVarselRepository,
    private val varselMeldingBygger: VarselMeldingBygger
) {
    private val logger = buildLogger
    private val mdc = MDC.getMDCAdapter()

    @WithSpan("hentVarsel")
    fun hentVarsel(varselId: UUID): VarselResponse = transaction {
        varselRepository.findByVarselId(varselId)?.asResponse() ?: throw VarselIkkeFunnetException("Varsel ikke funnet")
    }

    @WithSpan("finnVarsler")
    fun finnVarsler(
        periodeId: UUID,
        paging: Paging = Paging.none()
    ): List<VarselResponse> = transaction {
        varselRepository.findByPeriodeId(periodeId, paging).map { it.asResponse() }
    }

    @WithSpan("mottaPeriode")
    fun mottaPeriode(periode: Periode): List<VarselMelding> = transaction {
        try {
            mdc.eventName(periode.eventName)
            logger.debug("Prosesserer hendelse {}", periode.eventName)
            val periodeRow = periodeRepository.findByPeriodeId(periode.id)
            if (periodeRow != null) {
                if (periodeRow.avsluttetTimestamp != null) {
                    mdc.action(Action.IGNORE)
                    logger.warn("Ignorerer allerede avluttet periode {}", periode.id)
                    meterRegistry.periodeCounter(Action.IGNORE, periode)
                } else {
                    mdc.action(Action.UPDATE)
                    logger.debug("Oppdaterer periode {}", periode.id)
                    meterRegistry.periodeCounter(Action.UPDATE, periode)
                    val updatePeriodeRow = periode.asUpdatePeriodeRow()
                    periodeRepository.update(updatePeriodeRow)
                }
            } else {
                mdc.action(Action.INSERT)
                logger.debug("Oppretter periode {}", periode.id)
                meterRegistry.periodeCounter(Action.INSERT, periode)
                val insertPeriodeRow = periode.asInsertPeriodeRow()
                periodeRepository.insert(insertPeriodeRow)
            }

            if (periode.avsluttet != null) {
                if (applicationConfig.periodeVarslerEnabled) {
                    val varselRow = varselRepository.findByVarselId(periode.id)
                    if (varselRow != null) {
                        mdc.action(Action.IGNORE)
                        logger.debug(
                            "Varsel eksisterer allerede for avsluttet periode {}",
                            periode.id
                        )
                        emptyList()
                    } else {
                        mdc.action(Action.INSERT)
                        logger.debug(
                            "Oppretter og bestiller varsel for avsluttet periode {}",
                            periode.id
                        )
                        val insertVarselRow = periode.asInsertVarselRow()
                        varselRepository.insert(insertVarselRow)
                        periode.avsluttet.utfoertAv.type
                        val varsel = varselMeldingBygger.opprettPeriodeAvsluttetBeskjed(periode)
                        listOf(varsel)
                    }
                } else {
                    mdc.action(Action.IGNORE)
                    logger.warn("Utsendelse av varsler ved avsluttet periode er deaktivert")
                    emptyList()
                }
            } else {
                emptyList()
            }
        } finally {
            mdc.removeAll()
        }
    }

    @WithSpan("mottaBekreftelseHendelse")
    fun mottaBekreftelseHendelse(value: Pair<Periode?, BekreftelseHendelse?>): List<VarselMelding> = transaction {
        try {
            val (periode, hendelse) = value
            val eventType = if (hendelse != null) hendelse::class.java.name else "null"
            val eventName = hendelse?.hendelseType ?: "bekreftelse.null"
            mdc.eventName(eventName)
            logger.debug("Prosesserer hendelse {}", eventName)

            if (periode == null) {
                mdc.action(Action.FAIL)
                logger.warn("Ingen periode mottatt for hendelse {}", eventName)
                meterRegistry.bekreftelseHendelseCounter(Action.FAIL, eventType, eventName)
                throw PeriodeIkkeFunnetException("Ingen periode mottatt for hendelse $eventName")
            }

            when (hendelse) {
                is BekreftelseTilgjengelig -> {
                    val varselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
                    if (varselRow != null) {
                        mdc.action(Action.IGNORE)
                        logger.warn(
                            "Ignorerer siden varsel allerede finnes for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter(Action.IGNORE, hendelse)
                        emptyList()
                    } else {
                        if (applicationConfig.bekreftelseVarslerEnabled) {
                            mdc.action(Action.INSERT)
                            logger.debug(
                                "Oppretter og bestiller varsel for hendelse {} og periode {}",
                                hendelse.hendelseType,
                                hendelse.periodeId
                            )
                            meterRegistry.bekreftelseHendelseCounter(Action.INSERT, hendelse)
                            val insertVarselRow = hendelse.asInsertVarselRow()
                            varselRepository.insert(insertVarselRow)
                            val varsel = varselMeldingBygger.opprettBekreftelseTilgjengeligOppgave(periode, hendelse)
                            listOf(varsel)
                        } else {
                            mdc.action(Action.IGNORE)
                            logger.warn("Utsendelse av varsler ved tilgjengelig bekreftelse er deaktivert")
                            emptyList()
                        }
                    }
                }

                is BekreftelseMeldingMottatt -> {
                    val varselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
                    if (varselRow != null) {
                        mdc.action(Action.DELETE)
                        logger.debug(
                            "Avslutter og sletter varsel for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter(Action.DELETE, hendelse)
                        //varselRepository.deleteByVarselId(hendelse.bekreftelseId) TODO: Disablet sletting
                        listOf(varselMeldingBygger.avsluttVarsel(hendelse.bekreftelseId))
                    } else {
                        mdc.action(Action.FAIL)
                        logger.warn(
                            "Fant ingen varsel for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter(Action.FAIL, hendelse)
                        emptyList()
                    }
                }

                is PeriodeAvsluttet -> {
                    val varselRows = varselRepository.findByPeriodeIdAndVarselKilde(
                        periodeId = hendelse.periodeId,
                        varselKilde = VarselKilde.BEKREFTELSE_TILGJENGELIG
                    )
                    if (varselRows.isEmpty()) {
                        mdc.action(Action.FAIL)
                        logger.warn(
                            "Fant ingen varsler for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter(Action.FAIL, hendelse)
                        emptyList()
                    } else {
                        mdc.action(Action.DELETE)
                        logger.debug(
                            "Avlutter og sletter alle varsler for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter(Action.DELETE, hendelse)
                        /*varselRepository.deleteByPeriodeIdAndVarselKilde(
                            periodeId = hendelse.periodeId,
                            varselKilde = VarselKilde.BEKREFTELSE_TILGJENGELIG
                        ) TODO: Disablet sletting */
                        varselRows.map { varselMeldingBygger.avsluttVarsel(it.varselId) }
                    }
                }

                else -> {
                    if (hendelse != null) {
                        logger.debug("Ignorerer hendelse {}", hendelse.hendelseType)
                        meterRegistry.bekreftelseHendelseCounter(Action.IGNORE, hendelse)
                    } else {
                        logger.debug("Ignorerer hendelse som er null")
                        meterRegistry.bekreftelseHendelseCounter(Action.IGNORE, eventType, eventName)
                    }
                    emptyList()
                }
            }
        } finally {
            mdc.removeAll()
        }
    }

    @WithSpan("mottaVarselHendelse")
    fun mottaVarselHendelse(hendelse: VarselHendelse) = transaction {
        try {
            val eventName = "varsel.${hendelse.eventName.value}"
            mdc.eventName(eventName)
            logger.debug("Prosesserer hendelse {}", eventName)
            val varselId = UUID.fromString(hendelse.varselId)
            val varselRow = varselRepository.findByVarselId(varselId)
            if (varselRow != null) {
                if (hendelse.eventName == VarselEventName.EKSTERN_STATUS_OPPDATERT) {
                    if (varselRow.eksterntVarsel != null) {
                        if (hendelse.tidspunkt.isAfter(varselRow.eksterntVarsel.hendelseTimestamp)) {
                            mdc.action(Action.UPDATE)
                            logger.debug(
                                "Oppdaterer eksternt varsel for hendelse {} med type {} og status {}",
                                VarselHendelse::class.java.simpleName,
                                hendelse.varseltype,
                                hendelse.status
                            )
                            meterRegistry.varselHendelseCounter(Action.UPDATE, hendelse)
                            val updateEksterntVarselRow = hendelse.asUpdateEksterntVarselRow()
                            eksterntVarselRepository.update(updateEksterntVarselRow)
                        } else {
                            mdc.action(Action.IGNORE)
                            logger.warn(
                                "Ignorerer hendelse {} med type {} og status {} siden hendelse {} er eldre enn lagret eksternt varsel {}",
                                VarselHendelse::class.java.simpleName,
                                hendelse.varseltype,
                                hendelse.status,
                                hendelse.tidspunkt,
                                varselRow.hendelseTimestamp
                            )
                            meterRegistry.varselHendelseCounter(Action.IGNORE, hendelse)
                        }
                    } else {
                        mdc.action(Action.INSERT)
                        logger.debug(
                            "Oppretter eksternt varsel for hendelse {} med type {} og status {}",
                            VarselHendelse::class.java.simpleName,
                            hendelse.varseltype,
                            hendelse.status
                        )
                        meterRegistry.varselHendelseCounter(Action.INSERT, hendelse)
                        val insertEksterntVarselRow = hendelse.asInsertEksterntVarselRow()
                        eksterntVarselRepository.insert(insertEksterntVarselRow)
                    }
                } else {
                    if (hendelse.tidspunkt.isAfter(varselRow.hendelseTimestamp)) {
                        mdc.action(Action.UPDATE)
                        logger.debug(
                            "Oppdaterer varsel for hendelse {} med type {} og status {}",
                            VarselHendelse::class.java.simpleName,
                            hendelse.varseltype,
                            hendelse.status
                        )
                        meterRegistry.varselHendelseCounter(Action.UPDATE, hendelse)
                        val updateVarselRow = hendelse.asUpdateVarselRow()
                        varselRepository.update(updateVarselRow)
                    } else {
                        mdc.action(Action.IGNORE)
                        logger.warn(
                            "Ignorerer hendelse {} med type {} og status {} siden hendelse {} er eldre enn lagret varsel {}",
                            VarselHendelse::class.java.simpleName,
                            hendelse.varseltype,
                            hendelse.status,
                            hendelse.tidspunkt,
                            varselRow.hendelseTimestamp
                        )
                        meterRegistry.varselHendelseCounter(Action.IGNORE, hendelse)
                    }
                }
            } else {
                mdc.action(Action.FAIL)
                logger.warn("Fant ikke lagret varsel for hendelse {}", VarselHendelse::class.java.simpleName)
                meterRegistry.varselHendelseCounter(Action.FAIL, hendelse)
            }
        } finally {
            mdc.removeAll()
        }
    }
}