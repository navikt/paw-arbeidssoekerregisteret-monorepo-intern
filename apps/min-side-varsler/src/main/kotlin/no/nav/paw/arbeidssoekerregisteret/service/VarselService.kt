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
import no.nav.paw.arbeidssoekerregisteret.utils.bekreftelseHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.deleteAction
import no.nav.paw.arbeidssoekerregisteret.utils.deleteBekreftelseHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.eventName
import no.nav.paw.arbeidssoekerregisteret.utils.failAction
import no.nav.paw.arbeidssoekerregisteret.utils.failBekreftelseHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.failVarselHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.ignoreAction
import no.nav.paw.arbeidssoekerregisteret.utils.ignoreBekreftelseHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.ignorePeriodeCounter
import no.nav.paw.arbeidssoekerregisteret.utils.ignoreVarselHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.insertAction
import no.nav.paw.arbeidssoekerregisteret.utils.insertBekreftelseHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.insertPeriodeCounter
import no.nav.paw.arbeidssoekerregisteret.utils.insertVarselHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.readAction
import no.nav.paw.arbeidssoekerregisteret.utils.removeAll
import no.nav.paw.arbeidssoekerregisteret.utils.updateAction
import no.nav.paw.arbeidssoekerregisteret.utils.updatePeriodeCounter
import no.nav.paw.arbeidssoekerregisteret.utils.updateVarselHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.verboseEventName
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
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
        val eventName = periode.eventName
        try {
            mdc.eventName(eventName)
            mdc.readAction()
            logger.info("Prosesserer hendelse {}", eventName)
            val periodeRow = periodeRepository.findByPeriodeId(periode.id)
            if (periodeRow != null) {
                if (periodeRow.avsluttetTimestamp != null) {
                    mdc.ignoreAction()
                    logger.warn("Ignorerer allerede avluttet periode {}", periode.id)
                    meterRegistry.ignorePeriodeCounter(periode)
                } else {
                    mdc.updateAction()
                    logger.debug("Oppdaterer periode {}", periode.id)
                    meterRegistry.updatePeriodeCounter(periode)
                    val updatePeriodeRow = periode.asUpdatePeriodeRow()
                    periodeRepository.update(updatePeriodeRow)
                }
            } else {
                mdc.insertAction()
                logger.debug("Oppretter periode {}", periode.id)
                meterRegistry.insertPeriodeCounter(periode)
                val insertPeriodeRow = periode.asInsertPeriodeRow()
                periodeRepository.insert(insertPeriodeRow)
            }

            if (periode.avsluttet != null) {
                if (applicationConfig.periodeVarslerEnabled) {
                    val varselRow = varselRepository.findByVarselId(periode.id)
                    if (varselRow != null) {
                        mdc.ignoreAction()
                        logger.debug(
                            "Varsel eksisterer allerede for avsluttet periode {}",
                            periode.id
                        )
                        emptyList()
                    } else {
                        mdc.insertAction()
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
                    mdc.ignoreAction()
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
            val eventName = hendelse.eventName
            mdc.eventName(eventName)
            mdc.readAction()
            logger.info("Prosesserer hendelse {}", eventName)

            if (periode == null) {
                mdc.failAction()
                logger.warn("Ingen periode mottatt for hendelse {}", eventName)
                meterRegistry.failBekreftelseHendelseCounter(hendelse)
                throw PeriodeIkkeFunnetException("Ingen periode mottatt for hendelse $eventName")
            }

            when (hendelse) {
                is BekreftelseTilgjengelig -> mottaBekreftelseTilgjengelig(periode, hendelse)

                is BekreftelseMeldingMottatt -> mottaBekreftelseMeldingMottatt(hendelse)

                is PeriodeAvsluttet -> mottaPeriodeAvsluttetEllerPaaVegneAvStartet(hendelse)

                is BekreftelsePaaVegneAvStartet -> mottaPeriodeAvsluttetEllerPaaVegneAvStartet(hendelse)

                else -> mottaAnnenBekreftelseHendelse(hendelse)
            }
        } finally {
            mdc.removeAll()
        }
    }

    private fun mottaBekreftelseTilgjengelig(periode: Periode, hendelse: BekreftelseTilgjengelig): List<VarselMelding> {
        val varselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
        if (varselRow != null) {
            mdc.ignoreAction()
            logger.warn(
                "Ignorerer siden varsel allerede finnes for hendelse {} og periode {}",
                hendelse.hendelseType,
                hendelse.periodeId
            )
            meterRegistry.ignoreBekreftelseHendelseCounter(hendelse)
            return emptyList()
        } else {
            if (applicationConfig.bekreftelseVarslerEnabled) {
                mdc.insertAction()
                logger.debug(
                    "Oppretter og bestiller varsel for hendelse {} og periode {}",
                    hendelse.hendelseType,
                    hendelse.periodeId
                )
                meterRegistry.insertBekreftelseHendelseCounter(hendelse)
                val insertVarselRow = hendelse.asInsertVarselRow()
                varselRepository.insert(insertVarselRow)
                val varsel = varselMeldingBygger.opprettBekreftelseTilgjengeligOppgave(periode, hendelse)
                return listOf(varsel)
            } else {
                mdc.ignoreAction()
                logger.warn("Utsendelse av varsler ved tilgjengelig bekreftelse er deaktivert")
                return emptyList()
            }
        }
    }

    private fun mottaBekreftelseMeldingMottatt(hendelse: BekreftelseMeldingMottatt): List<VarselMelding> {
        val varselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
        if (varselRow != null) {
            mdc.deleteAction()
            logger.debug(
                "Avslutter og sletter varsel for hendelse {} og periode {}",
                hendelse.hendelseType,
                hendelse.periodeId
            )
            meterRegistry.deleteBekreftelseHendelseCounter(hendelse)
            //varselRepository.deleteByVarselId(hendelse.bekreftelseId) TODO: Disablet sletting
            return listOf(varselMeldingBygger.avsluttVarsel(hendelse.bekreftelseId))
        } else {
            mdc.failAction()
            logger.warn(
                "Fant ingen varsel for hendelse {} og periode {}",
                hendelse.hendelseType,
                hendelse.periodeId
            )
            meterRegistry.failBekreftelseHendelseCounter(hendelse)
            return emptyList()
        }
    }

    private fun mottaPeriodeAvsluttetEllerPaaVegneAvStartet(hendelse: BekreftelseHendelse): List<VarselMelding> {
        val varselRows = varselRepository.findByPeriodeIdAndVarselKilde(
            periodeId = hendelse.periodeId,
            varselKilde = VarselKilde.BEKREFTELSE_TILGJENGELIG
        )
        if (varselRows.isEmpty()) {
            mdc.failAction()
            logger.warn(
                "Fant ingen varsler for hendelse {} og periode {}",
                hendelse.hendelseType,
                hendelse.periodeId
            )
            meterRegistry.bekreftelseHendelseCounter(Action.FAIL, hendelse)
            return emptyList()
        } else {
            mdc.deleteAction()
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
            return varselRows.map { varselMeldingBygger.avsluttVarsel(it.varselId) }
        }
    }

    private fun mottaAnnenBekreftelseHendelse(hendelse: BekreftelseHendelse?): List<VarselMelding> {
        if (hendelse != null) {
            logger.debug("Ignorerer hendelse {}", hendelse.hendelseType)
            meterRegistry.bekreftelseHendelseCounter(Action.IGNORE, hendelse)
        } else {
            logger.debug("Ignorerer hendelse som er null")
            meterRegistry.bekreftelseHendelseCounter(Action.IGNORE, hendelse)
        }
        return emptyList()
    }

    @WithSpan("mottaVarselHendelse")
    fun mottaVarselHendelse(hendelse: VarselHendelse) = transaction {
        try {
            val eventName = hendelse.verboseEventName
            mdc.eventName(eventName)
            mdc.readAction()
            logger.info("Prosesserer hendelse {}", eventName)
            val varselId = UUID.fromString(hendelse.varselId)
            val varselRow = varselRepository.findByVarselId(varselId)
            if (varselRow != null) {
                if (hendelse.eventName == VarselEventName.EKSTERN_STATUS_OPPDATERT) {
                    if (varselRow.eksterntVarsel != null) {
                        if (hendelse.tidspunkt.isAfter(varselRow.eksterntVarsel.hendelseTimestamp)) {
                            mdc.updateAction()
                            logger.info(
                                "Oppdaterer eksternt varsel for hendelse {} med type {} og status {}",
                                VarselHendelse::class.java.simpleName,
                                hendelse.varseltype,
                                hendelse.status
                            )
                            meterRegistry.updateVarselHendelseCounter(hendelse)
                            val updateEksterntVarselRow = hendelse.asUpdateEksterntVarselRow()
                            eksterntVarselRepository.update(updateEksterntVarselRow)
                        } else {
                            mdc.ignoreAction()
                            logger.warn(
                                "Ignorerer hendelse {} med type {} og status {} siden hendelse {} er eldre enn lagret eksternt varsel {}",
                                VarselHendelse::class.java.simpleName,
                                hendelse.varseltype,
                                hendelse.status,
                                hendelse.tidspunkt,
                                varselRow.hendelseTimestamp
                            )
                            meterRegistry.ignoreVarselHendelseCounter(hendelse)
                        }
                    } else {
                        mdc.insertAction()
                        logger.info(
                            "Oppretter eksternt varsel for hendelse {} med type {} og status {}",
                            VarselHendelse::class.java.simpleName,
                            hendelse.varseltype,
                            hendelse.status
                        )
                        meterRegistry.insertVarselHendelseCounter(hendelse)
                        val insertEksterntVarselRow = hendelse.asInsertEksterntVarselRow()
                        eksterntVarselRepository.insert(insertEksterntVarselRow)
                    }
                } else {
                    if (hendelse.tidspunkt.isAfter(varselRow.hendelseTimestamp)) {
                        mdc.updateAction()
                        logger.info(
                            "Oppdaterer varsel for hendelse {} med type {} og status {}",
                            VarselHendelse::class.java.simpleName,
                            hendelse.varseltype,
                            hendelse.status
                        )
                        meterRegistry.updateVarselHendelseCounter(hendelse)
                        val updateVarselRow = hendelse.asUpdateVarselRow()
                        varselRepository.update(updateVarselRow)
                    } else {
                        mdc.ignoreAction()
                        logger.warn(
                            "Ignorerer hendelse {} med type {} og status {} siden hendelse {} er eldre enn lagret varsel {}",
                            VarselHendelse::class.java.simpleName,
                            hendelse.varseltype,
                            hendelse.status,
                            hendelse.tidspunkt,
                            varselRow.hendelseTimestamp
                        )
                        meterRegistry.ignoreVarselHendelseCounter(hendelse)
                    }
                }
            } else {
                mdc.failAction()
                logger.warn("Fant ikke lagret varsel for hendelse {}", VarselHendelse::class.java.simpleName)
                meterRegistry.failVarselHendelseCounter(hendelse)
            }
        } finally {
            mdc.removeAll()
        }
    }
}