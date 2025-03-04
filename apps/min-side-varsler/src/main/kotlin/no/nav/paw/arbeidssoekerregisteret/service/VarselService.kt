package no.nav.paw.arbeidssoekerregisteret.service

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
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.logging.logger.buildLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class VarselService(
    private val periodeRepository: PeriodeRepository,
    private val varselRepository: VarselRepository,
    private val varselMeldingBygger: VarselMeldingBygger
) {
    private val logger = buildLogger

    fun mottaPeriode(periode: Periode) = transaction {
        val periodeRow = periodeRepository.findByPeriodeId(periode.id)
        if (periodeRow != null) {
            logger.warn("Oppdaterer innslag for periode")
            val updatePeriodeRow = periode.asUpdatePeriodeRow()
            periodeRepository.update(updatePeriodeRow)
        } else {
            logger.debug("Oppretter innslag for periode")
            val insertPeriodeRow = periode.asInsertPeriodeRow()
            periodeRepository.insert(insertPeriodeRow)
        }
    }

    fun mottaBekreftelseHendelse(hendelse: BekreftelseHendelse): List<OppgaveMelding> = transaction {
        when (hendelse) {
            is BekreftelseTilgjengelig -> {
                val periodeRow = periodeRepository.findByPeriodeId(hendelse.periodeId)
                if (periodeRow != null) {
                    val varselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
                    if (varselRow != null) {
                        logger.warn(
                            "Ignorerer bekreftelse-hendelse av type {} siden varsel allerede finnes",
                            hendelse.hendelseType
                        )
                        emptyList()
                    } else {
                        logger.debug(
                            "Bestiller og oppretter varsel basert på bekreftelse-hendelse av type {}",
                            hendelse.hendelseType
                        )
                        val insertVarselRow = hendelse.asInsertVarselRow(
                            varselKilde = VarselKilde.BEKREFTELSE_TILGJENGELIG,
                            varselType = VarselType.OPPGAVE,
                        )
                        varselRepository.insert(insertVarselRow)

                        listOf(
                            varselMeldingBygger.opprettOppgave(
                                periodeRow.identitetsnummer,
                                hendelse.bekreftelseId,
                                hendelse.gjelderTil
                            )
                        )
                    }
                } else {
                    logger.warn("Fant ingen aktiv periode for bekreftelse-hendelse av type {}", hendelse.hendelseType)
                    emptyList()
                }
            }

            is BekreftelseMeldingMottatt -> {
                val varselRow = varselRepository.findByVarselId(hendelse.bekreftelseId)
                if (varselRow != null) {
                    logger.debug(
                        "Avslutter og sletter varsel for varsel på grunn av bekreftelse-hendelse av type {}",
                        hendelse.hendelseType
                    )
                    varselRepository.deleteByVarselId(hendelse.bekreftelseId)

                    listOf(varselMeldingBygger.avsluttOppgave(hendelse.bekreftelseId))
                } else {
                    logger.warn("Fant ingen varsel for bekreftelse-hendelse av type {}", hendelse.hendelseType)
                    emptyList()
                }
            }

            is PeriodeAvsluttet -> {
                logger.debug(
                    "Avlutter og sletter alle varsler på grunn av bekreftelse-hendelse av type {}",
                    hendelse.hendelseType
                )
                val varselRows = varselRepository.findByPeriodeId(hendelse.periodeId)
                varselRepository.deleteByPeriodeId(hendelse.periodeId)
                periodeRepository.deleteByPeriodeId(hendelse.periodeId)

                varselRows.map { varselMeldingBygger.avsluttOppgave(it.varselId) }
            }

            else -> {
                logger.debug("Ignorerer bekreftelse-hendelse av type {}", hendelse.hendelseType)
                emptyList()
            }
        }
    }

    fun mottaVarselHendelse(hendelse: VarselHendelse) = transaction {
        val varselRow = varselRepository.findByVarselId(UUID.fromString(hendelse.varselId))
        if (varselRow != null) {
            if (varselRow.hendelseTimestamp.isBefore(hendelse.tidspunkt)) {
                logger.debug("Oppdaterer varsel basert på varsel-hendelse med status {}", hendelse.status)
                val updateVarselRow = hendelse.asUpdateVarselRow()
                varselRepository.update(updateVarselRow)
            } else {
                logger.warn(
                    "Ignorerer varsel-hendelse med status {} siden lagret varsel {} er nyere enn hendelse {}",
                    hendelse.status,
                    varselRow.hendelseTimestamp,
                    hendelse.tidspunkt
                )
            }
        } else {
            logger.warn("Fant ikke lagret varsel for varsel-hendelse")
        }
    }
}