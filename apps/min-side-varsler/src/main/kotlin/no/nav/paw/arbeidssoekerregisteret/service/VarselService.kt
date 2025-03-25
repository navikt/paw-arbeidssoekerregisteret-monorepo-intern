package no.nav.paw.arbeidssoekerregisteret.service

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.models.VarselResponse
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.exception.VarselIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeHendelse
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
import no.nav.paw.arbeidssoekerregisteret.utils.tilNesteFredagKl9
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
                    val varselRows = varselRepository
                        .findByPeriodeIdAndVarselKilde(periode.id, VarselKilde.PERIODE_AVSLUTTET)
                    if (varselRows.isNotEmpty()) {
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
                        val varsel = varselMeldingBygger.opprettPeriodeAvsluttetBeskjed(
                            varselId = insertVarselRow.varselId,
                            identitetsnummer = periode.identitetsnummer,
                            avluttetAv = periode.avsluttet.utfoertAv.type
                        )
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
    fun mottaBekreftelseHendelse(periode: PeriodeHendelse, hendelse: BekreftelseHendelse): List<VarselMelding> =
        transaction {
            try {
                val eventName = hendelse.eventName
                mdc.eventName(eventName)
                mdc.readAction()
                logger.info("Prosesserer hendelse {}", eventName)

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

    private fun mottaBekreftelseTilgjengelig(
        periode: PeriodeHendelse,
        hendelse: BekreftelseTilgjengelig
    ): List<VarselMelding> {
        // TODO Deaktivere alle gamle varsler
        val gammelVarselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
        val avsluttGammeltVarsel: MutableList<VarselMelding> =
            if (gammelVarselRow != null) {
                logger.info("Inaktiverer varsel for bekreftelse {}", hendelse.bekreftelseId)
                mutableListOf(varselMeldingBygger.avsluttVarsel(hendelse.bekreftelseId))
            } else {
                mutableListOf()
            }

        val varselRow = varselRepository.findByBekreftelseId(hendelse.bekreftelseId)
        if (varselRow != null) {
            // TODO: Håndtere varsler som ikke er sendt pga feil når melding skulle legges på Kafka
            // TODO: Hvordan finne at melding ikke sendt? Status på eksternt varsel kanskje?
            mdc.ignoreAction()
            logger.warn(
                "Ignorerer siden varsel allerede finnes for hendelse {} og periode {}",
                hendelse.hendelseType,
                hendelse.periodeId
            )
            meterRegistry.ignoreBekreftelseHendelseCounter(hendelse)
            return avsluttGammeltVarsel //emptyList() TODO
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
                val varsel = varselMeldingBygger.opprettBekreftelseTilgjengeligOppgave(
                    varselId = insertVarselRow.varselId,
                    identitetsnummer = periode.identitetsnummer,
                    utsettEksternVarslingTil = hendelse.gjelderTil.tilNesteFredagKl9()
                )
                avsluttGammeltVarsel.add(varsel)
                return avsluttGammeltVarsel // listOf(varsel) TODO
            } else {
                mdc.ignoreAction()
                logger.warn("Utsendelse av varsler ved tilgjengelig bekreftelse er deaktivert")
                return avsluttGammeltVarsel // emptyList() TODO
            }
        }
    }

    private fun mottaBekreftelseMeldingMottatt(hendelse: BekreftelseMeldingMottatt): List<VarselMelding> {
        val varselRow = varselRepository.findByBekreftelseId(hendelse.bekreftelseId)
        if (varselRow != null) {
            mdc.deleteAction()
            logger.debug(
                "Avslutter og sletter varsel for hendelse {} og periode {}",
                hendelse.hendelseType,
                hendelse.periodeId
            )
            meterRegistry.deleteBekreftelseHendelseCounter(hendelse)
            return listOf(varselMeldingBygger.avsluttVarsel(varselRow.varselId))
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
        val varselRows = varselRepository
            .findByPeriodeIdAndVarselKilde(hendelse.periodeId, VarselKilde.BEKREFTELSE_TILGJENGELIG)
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
            return varselRows.map { varselMeldingBygger.avsluttVarsel(it.varselId) }
        }
    }

    private fun mottaAnnenBekreftelseHendelse(hendelse: BekreftelseHendelse): List<VarselMelding> {
        logger.debug("Ignorerer hendelse {}", hendelse.hendelseType)
        meterRegistry.bekreftelseHendelseCounter(Action.IGNORE, hendelse)
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
                                eventName,
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
                                eventName,
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
                            eventName,
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
                            eventName,
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
                            eventName,
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
                logger.warn("Fant ikke lagret varsel for hendelse {}", eventName)
                meterRegistry.failVarselHendelseCounter(hendelse)
            }
        } finally {
            mdc.removeAll()
        }
    }
}