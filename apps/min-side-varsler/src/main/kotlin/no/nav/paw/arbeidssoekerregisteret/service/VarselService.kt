package no.nav.paw.arbeidssoekerregisteret.service

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.models.VarselResponse
import no.nav.paw.arbeidssoekerregisteret.exception.PeriodeIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.exception.VarselIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselMelding
import no.nav.paw.arbeidssoekerregisteret.model.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.model.asInsertPeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.asInsertVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asResponse
import no.nav.paw.arbeidssoekerregisteret.model.asUpdatePeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.asUpdateVarselRow
import no.nav.paw.arbeidssoekerregisteret.repository.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.repository.VarselRepository
import no.nav.paw.arbeidssoekerregisteret.utils.bekreftelseHendelseCounter
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
    private val meterRegistry: MeterRegistry,
    private val periodeRepository: PeriodeRepository,
    private val varselRepository: VarselRepository,
    private val varselMeldingBygger: VarselMeldingBygger
) {
    private val logger = buildLogger

    fun hentVarsel(varselId: UUID): VarselResponse = transaction {
        varselRepository.findByVarselId(varselId)?.asResponse() ?: throw VarselIkkeFunnetException("Varsel ikke funnet")
    }

    fun finnVarsler(
        periodeId: UUID,
        paging: Paging = Paging.none()
    ): List<VarselResponse> = transaction {
        varselRepository.findByPeriodeId(periodeId, paging).map { it.asResponse() }
    }

    fun mottaPeriode(periode: Periode): List<VarselMelding> = transaction {
        try {
            MDC.put("x_event_name", if (periode.avsluttet == null) "periode.startet" else "periode.avsluttet")
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
                MDC.put("x_action", "insert")
                logger.debug(
                    "Oppretter og bestiller varsel for avsluttet periode {}",
                    periode.id
                )
                val insertVarselRow = periode.asInsertVarselRow()
                varselRepository.insert(insertVarselRow)
                listOf(
                    varselMeldingBygger.opprettPeriodeAvsluttetBeskjed(
                        varselId = periode.id,
                        identitetsnummer = periode.identitetsnummer
                    )
                )
            } else {
                emptyList()
            }
        } finally {
            MDC.remove("x_action")
            MDC.remove("x_event_name")
        }
    }

    fun mottaBekreftelseHendelse(value: Pair<Periode?, BekreftelseHendelse?>): List<VarselMelding> = transaction {
        try {
            val (periode, hendelse) = value
            val eventType = if (hendelse != null) hendelse::class.java.name else "null"
            val eventName = hendelse?.hendelseType ?: "bekreftelse.null"
            MDC.put("x_event_name", eventName)
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
                        MDC.put("x_action", "insert")
                        logger.debug(
                            "Oppretter og bestiller varsel for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter("insert", hendelse)
                        val insertVarselRow = hendelse.asInsertVarselRow()
                        varselRepository.insert(insertVarselRow)

                        listOf(
                            varselMeldingBygger.opprettBekreftelseTilgjengeligOppgave(
                                varselId = hendelse.bekreftelseId,
                                identitetsnummer = periode.identitetsnummer,
                                utsettEksternVarslingTil = hendelse.gjelderTil // TODO: Kalkuler SMS-dato
                            )
                        )
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
                        varselRepository.deleteByVarselId(hendelse.bekreftelseId)
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
                        varselRepository.deleteByPeriodeIdAndVarselKilde(
                            periodeId = hendelse.periodeId,
                            varselKilde = VarselKilde.BEKREFTELSE_TILGJENGELIG
                        )
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

    fun mottaVarselHendelse(hendelse: VarselHendelse) = transaction {
        try {
            MDC.put("x_event_name", "varsel.${hendelse.eventName.value}")

            val varselRow = varselRepository.findByVarselId(UUID.fromString(hendelse.varselId))
            if (varselRow != null) {
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