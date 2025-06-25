package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.model.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KonfliktRepository
import no.nav.paw.kafkakeygenerator.repository.PeriodeRepository
import no.nav.paw.logging.logger.buildLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class KonfliktService(
    applicationConfig: ApplicationConfig,
    private val identitetRepository: IdentitetRepository,
    private val konfliktRepository: KonfliktRepository,
    private val periodeRepository: PeriodeRepository,
    private val hendelseService: HendelseService
) {
    private val logger = buildLogger
    private val batchSize = applicationConfig.identitetKonfliktJob.batchSize

    fun lagreVentendeKonflikt(
        aktorId: String,
        type: KonfliktType,
        sourceTimestamp: Instant,
        identiteter: List<Identitet>
    ) {
        val identitetMergeRows = konfliktRepository.findByAktorIdAndType(aktorId, type)
        if (identitetMergeRows.isEmpty()) {
            val status = KonfliktStatus.VENTER
            val rowsAffected = konfliktRepository.insert(
                aktorId = aktorId,
                type = type,
                status = status,
                identiteter = identiteter,
                sourceTimestamp = sourceTimestamp
            )
            logger.info(
                "Lagret konflikt type {} med status {} (rows affected {})",
                type.name,
                status.name,
                rowsAffected
            )
        } else {
            logger.info("Innslag for konflikt type {} finnes allerede", type.name)
        }
    }

    fun handleVentendeMergeKonflikter() {
        val idList = konfliktRepository.updateStatusByTypeAndStatusReturning(
            type = KonfliktType.MERGE,
            fraStatus = KonfliktStatus.VENTER,
            tilStatus = KonfliktStatus.PROSESSERER,
            limit = batchSize
        )
        logger.info("Håndterer {} ventende idenitet-merges", idList.size)
        idList
            .mapNotNull { konfliktRepository.getById(it) }
            .forEach { handleVentendeMergeKonflikt(it.aktorId, it.type) }
    }

    private fun handleVentendeMergeKonflikt(
        aktorId: String,
        type: KonfliktType
    ) = transaction {
        val identitetRows = identitetRepository.findByAktorId(aktorId)
        if (identitetRows.isEmpty()) {
            throw IllegalStateException("Ingen identiteter funnet for aktorId")
        }

        val valgtArbeidssoekerId = uledAktivArbeidssoekerId(
            aktorId = aktorId,
            type = type,
            identitetRows = identitetRows
        )

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
            val identitetKonfliktRowsAffected = konfliktRepository
                .updateStatusByAktorIdAndType(
                    aktorId = aktorId,
                    type = type,
                    status = KonfliktStatus.FULLFOERT
                )
            logger.info(
                "Oppdaterer identitet-merge med status {} (rows affected {})",
                KonfliktStatus.FULLFOERT.name,
                identitetKonfliktRowsAffected
            )
            hendelseService.lagreIdentiteterEndretHendelse(
                aktorId = aktorId,
                arbeidssoekerId = valgtArbeidssoekerId,
                identiteter = identiteter,
                tidligereIdentiteter = tidligereIdentiteter
            )
            flyttetArbeidssoekerIdList.forEach { flyttetArbeidssoekerId ->
                hendelseService.lagreIdentiteterEndretHendelse(
                    aktorId = aktorId,
                    arbeidssoekerId = flyttetArbeidssoekerId,
                    identiteter = identiteter,
                    tidligereIdentiteter = tidligereIdentiteter
                )
            }
        }
    }

    private fun uledAktivArbeidssoekerId(
        aktorId: String,
        type: KonfliktType,
        identitetRows: List<IdentitetRow>
    ): Long? {
        val folkeregisterIdentiteter = identitetRows
            .filter { it.type == IdentitetType.FOLKEREGISTERIDENT }
            .map { it.identitet }
        val periodeRows = periodeRepository.findByIdentiteter(
            identitetList = folkeregisterIdentiteter
        )
        val aktivePeriodeRows = periodeRows.filter { it.avsluttetTimestamp == null }

        return if (periodeRows.isEmpty()) {
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
            // Flere aktive perioder. Kan ikke håndtere merge.
            val rowsAffected = konfliktRepository.updateStatusByAktorIdAndType(
                aktorId = aktorId,
                type = type,
                status = KonfliktStatus.FEILET
            )
            logger.error(
                "Kunne ikke løse merge for identiteter på grunn av at person har aktive perioder på forskjellige identiteter (rows affected {})",
                rowsAffected
            )
            null
        }
    }
}