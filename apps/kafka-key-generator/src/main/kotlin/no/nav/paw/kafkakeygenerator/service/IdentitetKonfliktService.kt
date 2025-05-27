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
        val identitetKonfliktRows = identitetKonfliktRepository.findByAktorId(aktorId)
        if (identitetKonfliktRows.isEmpty()) {
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
            logger.info("Innslag for identitet-konflikt finnes allerede")
        }
    }

    fun handleVentendeIdentitetKonflikter() {
        /*val identitetKonfliktRows = identitetKonfliktRepository
            .findByStatusForUpdate(status = IdentitetKonfliktStatus.VENTER, limit = 10)
        val aktorIdList = identitetKonfliktRows.map { it.aktorId }
        val rowsAffected = identitetKonfliktRepository.updateStatusByAktorId(
            aktorIdList = aktorIdList,
            status = IdentitetKonfliktStatus.PROSESSERER
        )
        if (rowsAffected != identitetKonfliktRows.size) {
            throw IllegalStateException("Fikk ikke endret status for alle valgte konflikter")
        }*/
        val idList = identitetKonfliktRepository.updateStatusByStatusReturning(
            fraStatus = IdentitetKonfliktStatus.VENTER,
            tilStatus = IdentitetKonfliktStatus.PROSESSERER,
        )
        logger.info("Håndterer {} ventende idenitet-konflikter", idList.size)
        idList
            .mapNotNull { id -> identitetKonfliktRepository.getById(id) }
            .map { it.aktorId }
            .forEach(::handleVentendeIdentitetKonflikt)
    }

    private fun handleVentendeIdentitetKonflikt(aktorId: String) = transaction {
        val identitetRows = identitetRepository.findByAktorId(aktorId)
        if (identitetRows.isEmpty()) {
            throw IllegalStateException("Ingen identiteter funnet for aktorId")
        }
        val identer = identitetRows
            .filter { it.type == IdentitetType.FOLKEREGISTERIDENT }
            .map { it.identitet }
        val periodeRows = periodeRepository.findByIdentiteter(identer)
        val aktivePeriodeRows = periodeRows.filter { it.avsluttetTimestamp == null }

        val valgtArbeidssoekerId = if (periodeRows.isEmpty()) {
            // Ingen perioder. Velger nyeste arbeidssoekerId.
            identitetRows.maxOf { it.arbeidssoekerId }
        } else if (aktivePeriodeRows.isEmpty()) {
            // Ingen aktive perioder. Velger arbeidssoekerId tilhørende nyeste periode.
            val nyestePeriode = periodeRows
                .maxBy { it.startetTimestamp.toEpochMilli() } // TODO: Finne siste sluttbrukeraksjon?
            val valgtIdentitet = identitetRows.find { it.identitet == nyestePeriode.identitet }
                ?: throw IllegalStateException("Fant ikke identitet for periode")
            valgtIdentitet.arbeidssoekerId
        } else if (aktivePeriodeRows.size == 1) {
            // Én aktiv periode. Velger tilhørende arbeidssoekerId.
            val aktivPeriode = aktivePeriodeRows.first()
            val valgtIdentitet = identitetRows.find { it.identitet == aktivPeriode.identitet }
                ?: throw IllegalStateException("Fant ikke identitet for periode")
            valgtIdentitet.arbeidssoekerId
        } else {
            // Flere aktive perioder. Kan ikke håndtere konflikt.
            val rowsAffected = identitetKonfliktRepository
                .updateStatusByAktorId(aktorId, IdentitetKonfliktStatus.KONFLIKT)
            logger.error(
                "Kunne ikke løse konflikt for identiteter på grunn av at person har aktive perioder på forskjellige identiteter (rows affected {})",
                rowsAffected
            )
            null
        }

        /*
         * database:
         *    key1 - identitet:
         *       - fnr1
         *       - aId1
         *    key2 - identitet:
         *       - fnr2
         *       - aId2
         *
         * aktor-hendelse:
         *    - fnr1
         *    - fnr2
         *    - fnr3 - g
         *
         * key1 - endret-hendelse:
         *    identiteter:
         *      - fnr1
         *      - fnr2
         *      - fnr3 - g
         *      - aId1 - g
         *      - aId2
         *    tidligereIdentiteter:
         *      - fnr1 - (g)
         *      - fnr2 - (g)
         *      - aId1 - (g)
         *      - aId2 - (g)
         *
         * key2 - flyttet-hendelse:
         *    identiteter:
         *
         *    tidligereIdentiteter:
         *       - fnr2 - g
         *       - fnr3
         *       - aId2 - g
         */

        if (valgtArbeidssoekerId != null) {
            val flyttetArbeidssoekerIdList = identitetRows
                .map { it.arbeidssoekerId }
                .filter { it != valgtArbeidssoekerId }
                .distinct()

            val identiteter = identitetRows
                .map { it.asIdentitet() }
                .toMutableList()
                .apply { add(valgtArbeidssoekerId.asIdentitet(gjeldende = true)) }
                .apply { flyttetArbeidssoekerIdList.forEach { add(it.asIdentitet(gjeldende = false)) } }
            val tidligereIdentiteter = identitetRows
                .filter { it.updatedTimestamp == null }
                .map { it.asIdentitet() }
                .toMutableList()
                .apply { add(valgtArbeidssoekerId.asIdentitet(gjeldende = true)) }
                .apply { flyttetArbeidssoekerIdList.forEach { add(it.asIdentitet(gjeldende = true)) } }

            val identitetRowsAffected = identitetRepository.updateArbeidssoekerIdAndStatusByAktorId(
                aktorId = aktorId,
                arbeidssoekerId = valgtArbeidssoekerId,
                status = IdentitetStatus.AKTIV
            )
            logger.info(
                "Oppdaterer identiteter med arbeidssoekerId og status {} (rows affected {})",
                IdentitetStatus.AKTIV.name,
                identitetRowsAffected
            )
            val identitetKonfliktRowsAffected = identitetKonfliktRepository
                .updateStatusByAktorId(
                    aktorId = aktorId,
                    status = IdentitetKonfliktStatus.FULLFOERT
                )
            logger.info(
                "Oppdaterer identitet-konflikt med status {} (rows affected {})",
                IdentitetKonfliktStatus.FULLFOERT.name,
                identitetKonfliktRowsAffected
            )
            identitetHendelseService.lagreIdentiteterEndretHendelse(
                aktorId = aktorId,
                arbeidssoekerId = valgtArbeidssoekerId,
                identiteter = identiteter,
                tidligereIdentiteter = tidligereIdentiteter
            )
            flyttetArbeidssoekerIdList.forEach { flyttetArbeidssoekerId ->
                identitetHendelseService.lagreIdentiteterEndretHendelse(
                    aktorId = aktorId,
                    arbeidssoekerId = flyttetArbeidssoekerId,
                    identiteter = identiteter,
                    tidligereIdentiteter = tidligereIdentiteter
                )
            }
        }
    }
}