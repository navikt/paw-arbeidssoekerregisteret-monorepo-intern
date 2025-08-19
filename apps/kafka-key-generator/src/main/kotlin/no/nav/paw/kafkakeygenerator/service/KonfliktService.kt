package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KafkaKeyRow
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysIdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KonfliktIdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KonfliktRepository
import no.nav.paw.kafkakeygenerator.repository.PeriodeRepository
import no.nav.paw.logging.logger.buildLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class KonfliktService(
    applicationConfig: ApplicationConfig,
    private val identitetRepository: IdentitetRepository,
    private val konfliktRepository: KonfliktRepository,
    private val konfliktIdentitetRepository: KonfliktIdentitetRepository,
    private val periodeRepository: PeriodeRepository,
    private val kafkaKeysIdentitetRepository: KafkaKeysIdentitetRepository,
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
        val nyeIdentiteterSet = identiteter.map { it.identitet }.toSet()
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
            val identitetMergeRow = identitetMergeRows.first()
            val konfliktId = identitetMergeRow.id
            val eksisterendeIdeniteterSet = identitetMergeRow.identiteter.map { it.identitet }.toSet()

            val rowsAffected1 = identitetMergeRow.identiteter
                .map { it.identitet }
                .filter { !nyeIdentiteterSet.contains(it) }
                .sumOf { identitet ->
                    konfliktIdentitetRepository
                        .deleteByKonfliktIdAndIdentitet(konfliktId, identitet)
                }

            val rowsAffected2 = identiteter
                .filter { !eksisterendeIdeniteterSet.contains(it.identitet) }
                .sumOf { identitet ->
                    konfliktIdentitetRepository.insert(konfliktId, identitet)
                }
            logger.info(
                "Oppdaterte konflikt type {} ({} rows deleted, {} rows inserted)",
                type.name,
                rowsAffected1,
                rowsAffected2
            )
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
            .forEach {
                handleVentendeMergeKonflikt(
                    aktorId = it.aktorId,
                    type = it.type,
                    sourceTimestamp = it.sourceTimestamp
                )
            }
    }

    private fun handleVentendeMergeKonflikt(
        aktorId: String,
        type: KonfliktType,
        sourceTimestamp: Instant
    ) = transaction {
        val eksisterendeIdentitetRows = identitetRepository.findByAktorId(aktorId)
        val konfliktIdentitetRows = konfliktIdentitetRepository.findByAktorId(aktorId)
        val ideniteter = eksisterendeIdentitetRows
            .filter { it.status != IdentitetStatus.SLETTET }
            .map { it.asIdentitet() }
            .toMutableSet()
        ideniteter += konfliktIdentitetRows
            .map { it.asIdentitet() }
            .toSet()

        val identitetSet = ideniteter
            .map { it.identitet }
            .toSet()
        val eksisterendeIdentitetSet = eksisterendeIdentitetRows
            .map { it.identitet }
            .toSet()
        val eksisterendeKafkaKeyRows = kafkaKeysIdentitetRepository
            .findByIdentiteter(identiteter = identitetSet)

        val valgtArbeidssoekerId = uledAktivArbeidssoekerId(
            aktorId = aktorId,
            type = type,
            eksisterendeKafkaKeyRows = eksisterendeKafkaKeyRows
        )

        if (valgtArbeidssoekerId != null) {
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

            konfliktIdentitetRows
                .filter { !eksisterendeIdentitetSet.contains(it.identitet) }
                .forEach {
                    val rowsAffected = identitetRepository.insert(
                        arbeidssoekerId = valgtArbeidssoekerId,
                        aktorId = aktorId,
                        identitet = it.identitet,
                        type = it.type,
                        gjeldende = it.gjeldende,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = sourceTimestamp
                    )
                    logger.info(
                        "Oppretter identitet av type {} med arbeidssoekerId og status {} (rows affected {})",
                        it.type.name,
                        IdentitetStatus.AKTIV.name,
                        rowsAffected
                    )
                }

            val arbeidssoekerIdSet = eksisterendeKafkaKeyRows
                .map { it.arbeidssoekerId }
                .toSet()

            arbeidssoekerIdSet
                .sorted()
                .forEach { arbeidssoekerId ->
                    val tidligereIdentiteter = eksisterendeIdentitetRows
                        .filter { it.arbeidssoekerId == arbeidssoekerId }
                        .filter { it.status != IdentitetStatus.SLETTET }
                        .map { it.asIdentitet() }
                        .toMutableList()
                        .apply { add(arbeidssoekerId.asIdentitet(gjeldende = true)) }

                    val identiteter = if (arbeidssoekerId == valgtArbeidssoekerId) {
                        ideniteter
                            .toMutableList()
                            .apply {
                                arbeidssoekerIdSet
                                    .sorted()
                                    .map { it.asIdentitet(gjeldende = (it == valgtArbeidssoekerId)) }
                                    .sortedBy { it.identitet }
                                    .forEach { add(it) }
                            }
                    } else {
                        emptyList()
                    }

                    hendelseService.lagreIdentiteterMergetHendelse(
                        aktorId = aktorId,
                        arbeidssoekerId = arbeidssoekerId,
                        identiteter = identiteter.sortedBy { it.type.ordinal },
                        tidligereIdentiteter = tidligereIdentiteter.sortedBy { it.type.ordinal }
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
        eksisterendeKafkaKeyRows: List<KafkaKeyRow>
    ): Long? {
        val identiteter = eksisterendeKafkaKeyRows
            .map { it.identitetsnummer }
        val periodeRows = periodeRepository.findByIdentiteter(
            identitetList = identiteter
        )
        val aktivePeriodeRows = periodeRows.filter { it.avsluttetTimestamp == null }

        return if (periodeRows.isEmpty()) {
            // Ingen perioder. Velger nyeste arbeidssoekerId.
            eksisterendeKafkaKeyRows.maxOf { it.arbeidssoekerId }
        } else if (aktivePeriodeRows.isEmpty()) {
            // Ingen aktive perioder. Velger arbeidssoekerId tilhørende nyeste periode.
            val nyestePeriode = periodeRows
                .maxBy { it.startetTimestamp.toEpochMilli() } // TODO: Finne siste sluttbrukeraksjon?
            val valgtIdentitet = eksisterendeKafkaKeyRows.find { it.identitetsnummer == nyestePeriode.identitet }
                ?: throw IllegalStateException("Fant ikke identitet for periode")
            valgtIdentitet.arbeidssoekerId
        } else if (aktivePeriodeRows.size == 1) {
            // Én aktiv periode. Velger tilhørende arbeidssoekerId.
            val aktivPeriode = aktivePeriodeRows.first()
            val valgtIdentitet = eksisterendeKafkaKeyRows.find { it.identitetsnummer == aktivPeriode.identitet }
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