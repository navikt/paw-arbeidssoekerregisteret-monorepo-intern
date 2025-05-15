package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentitetKonfliktStatus
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.repository.IdentitetKonfliktRepository
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.PeriodeRepository
import no.nav.paw.logging.logger.buildLogger
import org.jetbrains.exposed.sql.transactions.transaction

class IdentitetKonfliktService(
    private val identitetRepository: IdentitetRepository,
    private val identitetKonfliktRepository: IdentitetKonfliktRepository,
    private val periodeRepository: PeriodeRepository,
    private val identitetHendelseService: IdentitetHendelseService
) {
    private val logger = buildLogger

    fun lagreVentendeIdentitetKonflikt(
        aktorId: String,
    ) {
        val rows = identitetKonfliktRepository.findByAktorId(aktorId)
        if (rows.isEmpty()) {
            val rowsAffected = identitetKonfliktRepository.insert(
                aktorId = aktorId,
                status = IdentitetKonfliktStatus.VENTER
            )
            logger.info(
                "Lagret identitet-konflikt med status {} (rows affected {})",
                IdentitetKonfliktStatus.VENTER.name,
                rowsAffected
            )
        } else {
            logger.info("Innskag for identitet-konflikt finnes allerede")
        }
    }

    fun handleVentendeIdentitetKonflikter() = transaction {
        val konflikter = identitetKonfliktRepository
            .findByStatusForUpdate(status = IdentitetKonfliktStatus.VENTER, limit = 10)
        val aktorIdList = konflikter.map { it.aktorId }
        val rowsAffected = identitetKonfliktRepository.updateStatusByAktorId(
            aktorIdList = aktorIdList,
            status = IdentitetKonfliktStatus.PROSESSERER
        )
        if (rowsAffected != konflikter.size) {
            throw IllegalStateException("Fikk ikke endret status for alle valgte konflikter")
        }
        aktorIdList.forEach(::handleVentendeIdentitetKonflikt)
    }

    private fun handleVentendeIdentitetKonflikt(aktorId: String) {
        val identitetRows = identitetRepository.findByAktorId(aktorId)
        if (identitetRows.isEmpty()) {
            throw IllegalStateException("Ingen identiteter funnet for aktorId")
        }
        val identer = identitetRows
            .filter { it.type == IdentitetType.FOLKEREGISTERIDENT }
            .map { it.identitet }
        val periodeRows = periodeRepository.findByIdentiteter(identer)
        val aktivePeriodeRows = periodeRows.filter { it.avsluttetTimestamp == null }
        val valgArbeidssoekerId = if (aktivePeriodeRows.isEmpty()) {
            identitetRows.maxOf { it.arbeidssoekerId }
        } else if (aktivePeriodeRows.size == 1) {
        } else {
            null
        }

        if (aktivePeriodeRows.size <= 1) {
            val nyesteArbeidssoekerId = identitetRows.maxOf { it.arbeidssoekerId }
            val rowsAffected = identitetRepository.updateArbeidssoekerIdAndStatusByAktorId(
                aktorId = aktorId,
                arbeidssoekerId = nyesteArbeidssoekerId,
                status = IdentitetStatus.AKTIV
            )
            logger.info(
                "Oppdaterer identiteter med arbeidssoekerId og status {} (rows affected {})",
                IdentitetStatus.AKTIV.name,
                rowsAffected
            )
            identitetHendelseService.lagreVentendeIdentitetHendelse(
                aktorId = aktorId,
                arbeidssoekerId = nyesteArbeidssoekerId,
                identiteter = identitetRows.map { it.asIdentitet() }.toMutableList(),
                tidligereIdentiteter = mutableListOf() // TODO: Hvordan sette denne korrekt?
            )
        } else {
        }
    }
}