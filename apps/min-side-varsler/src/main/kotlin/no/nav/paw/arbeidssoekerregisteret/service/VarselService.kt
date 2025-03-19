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
import no.nav.paw.arbeidssoekerregisteret.utils.bekreftelseHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.tilNesteFredagKl9
import no.nav.paw.arbeidssoekerregisteret.utils.periodeCounter
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
            val eventName = if (periode.avsluttet == null) "periode.startet" else "periode.avsluttet"
            MDC.put("x_event_name", eventName)
            logger.debug("Prosesserer hendelse {}", eventName)
            val periodeRow = periodeRepository.findByPeriodeId(periode.id)
            if (periodeRow != null) {
                if (periodeRow.avsluttetTimestamp != null) {
                    MDC.put("x_action", "ignore")
                    logger.warn("Ignorerer allerede avluttet periode {}", periode.id)
                    meterRegistry.periodeCounter("ignore", periode)
                } else {
                    MDC.put("x_action", "update")
                    logger.debug("Oppdaterer periode {}", periode.id)
                    meterRegistry.periodeCounter("update", periode)
                    val updatePeriodeRow = periode.asUpdatePeriodeRow()
                    periodeRepository.update(updatePeriodeRow)
                }
            } else {
                MDC.put("x_action", "insert")
                logger.debug("Oppretter periode {}", periode.id)
                meterRegistry.periodeCounter("insert", periode)
                val insertPeriodeRow = periode.asInsertPeriodeRow()
                periodeRepository.insert(insertPeriodeRow)
            }

            if (periode.avsluttet != null) {
                if (applicationConfig.periodeVarslerEnabled) {
                    val varselRow = varselRepository.findByVarselId(periode.id)
                    if (varselRow != null) {
                        MDC.put("x_action", "ignore")
                        logger.debug(
                            "Varsel eksisterer allerede for avsluttet periode {}",
                            periode.id
                        )
                        emptyList()
                    } else {
                        MDC.put("x_action", "insert")
                        logger.debug(
                            "Oppretter og bestiller varsel for avsluttet periode {}",
                            periode.id
                        )
                        val insertVarselRow = periode.asInsertVarselRow()
                        varselRepository.insert(insertVarselRow)
                        val varsel = varselMeldingBygger.opprettPeriodeAvsluttetBeskjed(
                            varselId = periode.id,
                            identitetsnummer = periode.identitetsnummer
                        )
                        listOf(varsel)
                    }
                } else {
                    MDC.put("x_action", "ignore")
                    logger.warn("Utsendelse av varsler ved avsluttet periode er deaktivert")
                    emptyList()
                }
            } else {
                emptyList()
            }
        } finally {
            MDC.remove("x_action")
            MDC.remove("x_event_name")
        }
    }

    @WithSpan("mottaBekreftelseHendelse")
    fun mottaBekreftelseHendelse(value: Pair<Periode?, BekreftelseHendelse?>): List<VarselMelding> = transaction {
        try {
            val (periode, hendelse) = value
            val eventType = if (hendelse != null) hendelse::class.java.name else "null"
            val eventName = hendelse?.hendelseType ?: "bekreftelse.null"
            MDC.put("x_event_name", eventName)
            logger.debug("Prosesserer hendelse {}", eventName)

            if (periode == null) {
                MDC.put("x_action", "fail")
                logger.warn("Ingen periode mottatt for hendelse {}", eventName)
                meterRegistry.bekreftelseHendelseCounter("fail", eventType, eventName)
                throw PeriodeIkkeFunnetException("Ingen periode mottatt for hendelse $eventName")
            }

            when (hendelse) {
                is BekreftelseTilgjengelig -> {
                    val varselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
                    if (varselRow != null) {
                        MDC.put("x_action", "ignore")
                        logger.warn(
                            "Ignorerer siden varsel allerede finnes for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter("ignore", hendelse)
                        emptyList()
                    } else {
                        if (applicationConfig.bekreftelseVarslerEnabled) {
                            MDC.put("x_action", "insert")
                            logger.debug(
                                "Oppretter og bestiller varsel for hendelse {} og periode {}",
                                hendelse.hendelseType,
                                hendelse.periodeId
                            )
                            meterRegistry.bekreftelseHendelseCounter("insert", hendelse)
                            val insertVarselRow = hendelse.asInsertVarselRow()
                            varselRepository.insert(insertVarselRow)
                            val varsel = varselMeldingBygger.opprettBekreftelseTilgjengeligOppgave(
                                varselId = hendelse.bekreftelseId,
                                identitetsnummer = periode.identitetsnummer,
                                utsettEksternVarslingTil = hendelse.gjelderTil.tilNesteFredagKl9()
                            )
                            listOf(varsel)
                        } else {
                            MDC.put("x_action", "ignore")
                            logger.warn("Utsendelse av varsler ved tilgjengelig bekreftelse er deaktivert")
                            emptyList()
                        }
                    }
                }

                is BekreftelseMeldingMottatt -> {
                    val varselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
                    if (varselRow != null) {
                        MDC.put("x_action", "delete")
                        logger.debug(
                            "Avslutter og sletter varsel for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter("delete", hendelse)
                        //varselRepository.deleteByVarselId(hendelse.bekreftelseId)
                        listOf(varselMeldingBygger.avsluttVarsel(hendelse.bekreftelseId))
                    } else {
                        MDC.put("x_action", "fail")
                        logger.warn(
                            "Fant ingen varsel for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter("fail", hendelse)
                        emptyList()
                    }
                }

                is PeriodeAvsluttet -> {
                    val varselRows = varselRepository.findByPeriodeIdAndVarselKilde(
                        periodeId = hendelse.periodeId,
                        varselKilde = VarselKilde.BEKREFTELSE_TILGJENGELIG
                    )
                    if (varselRows.isEmpty()) {
                        MDC.put("x_action", "fail")
                        logger.warn(
                            "Fant ingen varsler for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter("fail", hendelse)
                        emptyList()
                    } else {
                        MDC.put("x_action", "delete")
                        logger.debug(
                            "Avlutter og sletter alle varsler for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter("delete", hendelse)
                        /*varselRepository.deleteByPeriodeIdAndVarselKilde(
                            periodeId = hendelse.periodeId,
                            varselKilde = VarselKilde.BEKREFTELSE_TILGJENGELIG
                        )*/
                        varselRows.map { varselMeldingBygger.avsluttVarsel(it.varselId) }
                    }
                }

                else -> {
                    if (hendelse != null) {
                        logger.debug("Ignorerer hendelse {}", hendelse.hendelseType)
                        meterRegistry.bekreftelseHendelseCounter("ignore", hendelse)
                    } else {
                        logger.debug("Ignorerer hendelse som er null")
                        meterRegistry.bekreftelseHendelseCounter("ignore", eventType, eventName)
                    }
                    emptyList()
                }
            }
        } finally {
            MDC.remove("x_action")
            MDC.remove("x_event_name")
        }
    }

    @WithSpan("mottaVarselHendelse")
    fun mottaVarselHendelse(hendelse: VarselHendelse) = transaction {
        try {
            val eventName = "varsel.${hendelse.eventName.value}"
            MDC.put("x_event_name", eventName)
            logger.debug("Prosesserer hendelse {}", eventName)
            val varselId = UUID.fromString(hendelse.varselId)
            val varselRow = varselRepository.findByVarselId(varselId)
            if (varselRow != null) {
                if (hendelse.eventName == VarselEventName.EKSTERN_STATUS_OPPDATERT) {
                    if (varselRow.eksterntVarsel != null) {
                        if (hendelse.tidspunkt.isAfter(varselRow.eksterntVarsel.hendelseTimestamp)) {
                            MDC.put("x_action", "update")
                            logger.debug(
                                "Oppdaterer eksternt varsel for hendelse {} med type {} og status {}",
                                VarselHendelse::class.java.simpleName,
                                hendelse.varseltype,
                                hendelse.status
                            )
                            meterRegistry.varselHendelseCounter("update", hendelse)
                            val updateEksterntVarselRow = hendelse.asUpdateEksterntVarselRow()
                            eksterntVarselRepository.update(updateEksterntVarselRow)
                        } else {
                            MDC.put("x_action", "ignore")
                            logger.warn(
                                "Ignorerer hendelse {} med type {} og status {} siden hendelse {} er eldre enn lagret eksternt varsel {}",
                                VarselHendelse::class.java.simpleName,
                                hendelse.varseltype,
                                hendelse.status,
                                hendelse.tidspunkt,
                                varselRow.hendelseTimestamp
                            )
                            meterRegistry.varselHendelseCounter("ignore", hendelse)
                        }
                    } else {
                        MDC.put("x_action", "insert")
                        logger.debug(
                            "Oppretter eksternt varsel for hendelse {} med type {} og status {}",
                            VarselHendelse::class.java.simpleName,
                            hendelse.varseltype,
                            hendelse.status
                        )
                        meterRegistry.varselHendelseCounter("insert", hendelse)
                        val insertEksterntVarselRow = hendelse.asInsertEksterntVarselRow()
                        eksterntVarselRepository.insert(insertEksterntVarselRow)
                    }
                } else {
                    if (hendelse.tidspunkt.isAfter(varselRow.hendelseTimestamp)) {
                        MDC.put("x_action", "update")
                        logger.debug(
                            "Oppdaterer varsel for hendelse {} med type {} og status {}",
                            VarselHendelse::class.java.simpleName,
                            hendelse.varseltype,
                            hendelse.status
                        )
                        meterRegistry.varselHendelseCounter("update", hendelse)
                        val updateVarselRow = hendelse.asUpdateVarselRow()
                        varselRepository.update(updateVarselRow)
                    } else {
                        MDC.put("x_action", "ignore")
                        logger.warn(
                            "Ignorerer hendelse {} med type {} og status {} siden hendelse {} er eldre enn lagret varsel {}",
                            VarselHendelse::class.java.simpleName,
                            hendelse.varseltype,
                            hendelse.status,
                            hendelse.tidspunkt,
                            varselRow.hendelseTimestamp
                        )
                        meterRegistry.varselHendelseCounter("ignore", hendelse)
                    }
                }
            } else {
                MDC.put("x_action", "fail")
                logger.warn("Fant ikke lagret varsel for hendelse {}", VarselHendelse::class.java.simpleName)
                meterRegistry.varselHendelseCounter("fail", hendelse)
            }
        } finally {
            MDC.remove("x_action")
            MDC.remove("x_event_name")
        }
    }
}