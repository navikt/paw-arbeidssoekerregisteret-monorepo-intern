package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.model.*
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
        val eksisterendeIdentitetRows = identitetRepository.findByAktorId(aktorId)
        if (eksisterendeIdentitetRows.isEmpty()) {
            throw IllegalStateException("Ingen identiteter funnet for aktorId")
        }

        val valgtArbeidssoekerId = uledAktivArbeidssoekerId(
            aktorId = aktorId,
            type = type,
            identitetRows = eksisterendeIdentitetRows
        )

        if (valgtArbeidssoekerId != null) {
            val arbeidssoekerIdList = eksisterendeIdentitetRows
                .map { it.arbeidssoekerId }
                .distinct()

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

            val nyeIdentiteter = eksisterendeIdentitetRows
                .map { it.asIdentitet() }
                .toMutableList()
                .apply {
                    arbeidssoekerIdList
                        .map { it.asIdentitet(gjeldende = (it == valgtArbeidssoekerId)) }
                        .sortedBy { it.identitet }
                        .forEach { add(it) }
                }

            arbeidssoekerIdList.forEach { arbeidssoekerId ->
                val tidligereIdentiteter = eksisterendeIdentitetRows
                    .filter { it.arbeidssoekerId == arbeidssoekerId }
                    .filter { it.status != IdentitetStatus.SLETTET }
                    .map { it.asIdentitet() }
                    .toMutableList()
                    .apply { add(arbeidssoekerId.asIdentitet(gjeldende = true)) }

                val identiteter = if (arbeidssoekerId == valgtArbeidssoekerId) {
                    nyeIdentiteter
                } else {
                    emptyList()
                }

                hendelseService.lagreIdentiteterMergetHendelse(
                    aktorId = aktorId,
                    arbeidssoekerId = arbeidssoekerId,
                    identiteter = identiteter,
                    tidligereIdentiteter = tidligereIdentiteter
                )
            }

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