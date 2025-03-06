package no.nav.paw.arbeidssoekerregisteret.service

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.model.OppgaveMelding
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.model.asInsertPeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.asInsertVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asUpdatePeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.asUpdateVarselRow
import no.nav.paw.arbeidssoekerregisteret.repository.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.repository.VarselRepository
import no.nav.paw.arbeidssoekerregisteret.utils.bekreftelseHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.periodeCounter
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

    fun mottaPeriode(periode: Periode) = transaction {
        try {
            MDC.put("x_event_name", if (periode.avsluttet == null) "periode.startet" else "periode.avsluttet")
            val periodeRow = periodeRepository.findByPeriodeId(periode.id)
            if (periodeRow != null) {
                logger.debug("Oppdaterer periode {}", periode.id)
                meterRegistry.periodeCounter("update", periode)
                val updatePeriodeRow = periode.asUpdatePeriodeRow()
                periodeRepository.update(updatePeriodeRow)
            } else {
                logger.debug("Oppretter periode {}", periode.id)
                meterRegistry.periodeCounter("insert", periode)
                val insertPeriodeRow = periode.asInsertPeriodeRow()
                periodeRepository.insert(insertPeriodeRow)
            }
        } finally {
            MDC.remove("x_event_name")
        }
    }

    fun mottaBekreftelseHendelse(value: Pair<Periode?, BekreftelseHendelse?>): List<OppgaveMelding> = transaction {
        try {
            val (periode, hendelse) = value
            MDC.put("x_event_name", hendelse?.hendelseType ?: "null")

            when (hendelse) {
                is BekreftelseTilgjengelig -> {
                    if (periode != null) {
                        val varselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
                        if (varselRow != null) {
                            logger.warn(
                                "Ignorerer siden varsel allerede finnes for hendelse {} og periode {}",
                                hendelse.hendelseType,
                                hendelse.periodeId
                            )
                            meterRegistry.bekreftelseHendelseCounter("ignore", hendelse)
                            emptyList()
                        } else {
                            logger.debug(
                                "Bestiller og oppretter varsel for hendelse {} og periode {}",
                                hendelse.hendelseType,
                                hendelse.periodeId
                            )
                            meterRegistry.bekreftelseHendelseCounter("insert", hendelse)
                            val insertVarselRow = hendelse.asInsertVarselRow(
                                varselKilde = VarselKilde.BEKREFTELSE_TILGJENGELIG,
                                varselType = VarselType.OPPGAVE,
                            )
                            varselRepository.insert(insertVarselRow)

                            listOf(
                                varselMeldingBygger.opprettOppgave(
                                    periode.identitetsnummer,
                                    hendelse.bekreftelseId,
                                    hendelse.gjelderTil
                                )
                            )
                        }
                    } else {
                        logger.warn(
                            "Ingen periode mottatt for hendelse {} med periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter("fail", hendelse)
                        emptyList()
                    }
                }

                is BekreftelseMeldingMottatt -> {
                    val varselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
                    if (varselRow != null) {
                        logger.debug(
                            "Avslutter og sletter varsel for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter("delete", hendelse)
                        varselRepository.deleteByVarselId(hendelse.bekreftelseId)
                        listOf(varselMeldingBygger.avsluttOppgave(hendelse.bekreftelseId))
                    } else {
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
                    val varselRows = varselRepository.findByPeriodeId(hendelse.periodeId)
                    if (varselRows.isEmpty()) {
                        logger.warn(
                            "Fant ingen varsler for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter("fail", hendelse)
                        emptyList()
                    } else {
                        logger.debug(
                            "Avlutter og sletter alle varsler for hendelse {} og periode {}",
                            hendelse.hendelseType,
                            hendelse.periodeId
                        )
                        meterRegistry.bekreftelseHendelseCounter("delete", hendelse)
                        varselRepository.deleteByPeriodeId(hendelse.periodeId)
                        varselRows.map { varselMeldingBygger.avsluttOppgave(it.varselId) }
                    }
                }

                else -> {
                    if (hendelse != null) {
                        logger.debug("Ignorerer hendelse {}", hendelse.hendelseType)
                        meterRegistry.bekreftelseHendelseCounter("ignore", hendelse)
                    } else {
                        logger.debug("Ignorerer hendelse som er null")
                        meterRegistry.bekreftelseHendelseCounter("ignore", "null", "bekreftelse.null")
                    }
                    emptyList()
                }
            }
        } finally {
            MDC.remove("x_event_name")
        }
    }

    fun mottaVarselHendelse(hendelse: VarselHendelse) = transaction {
        try {
            MDC.put("x_event_name", "varsel.${hendelse.eventName.value}")

            val varselRow = varselRepository.findByVarselId(UUID.fromString(hendelse.varselId))
            if (varselRow != null) {
                if (varselRow.hendelseTimestamp.isBefore(hendelse.tidspunkt)) {
                    logger.debug(
                        "Oppdaterer varsel for hendelse {} med type {} og status {}",
                        VarselHendelse::class.java.simpleName,
                        hendelse.varseltype,
                        hendelse.status
                    )
                    val updateVarselRow = hendelse.asUpdateVarselRow()
                    varselRepository.update(updateVarselRow)
                } else {
                    logger.warn(
                        "Ignorerer hendelse {} med type {} og status {} siden lagret varsel {} er nyere enn hendelse {}",
                        VarselHendelse::class.java.simpleName,
                        hendelse.varseltype,
                        hendelse.status,
                        varselRow.hendelseTimestamp,
                        hendelse.tidspunkt
                    )
                }
            } else {
                logger.warn("Fant ikke lagret varsel for hendelse {}", VarselHendelse::class.java.simpleName)
            }
        } finally {
            MDC.remove("x_event_name")
        }
    }
}