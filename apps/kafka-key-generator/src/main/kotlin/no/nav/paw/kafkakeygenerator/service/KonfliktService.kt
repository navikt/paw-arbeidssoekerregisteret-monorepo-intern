package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
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

    fun slettVentendeKonflikter(
        aktorId: String
    ) {
        konfliktRepository.updateStatusByAktorIdAndStatus(
            aktorId = aktorId,
            fraStatus = KonfliktStatus.VENTER,
            tilStatus = KonfliktStatus.SLETTET
        )
    }

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
        logger.info("Håndterer {} ventende identitet-merges", idList.size)
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
        val alleIdentitetSet = eksisterendeIdentitetRows
            .filter { it.status != IdentitetStatus.SLETTET }
            .map { it.identitet }
            .toSet() + konfliktIdentitetRows
            .map { it.identitet }
            .toSet()
        val eksisterendeIdentitetSet = eksisterendeIdentitetRows
            .map { it.identitet }
            .toSet()
        val konfliktIdentitetSet = konfliktIdentitetRows
            .map { it.identitet }
            .toSet()
        val eksisterendeKafkaKeyRows = kafkaKeysIdentitetRepository
            .findByIdentiteter(identiteter = alleIdentitetSet)
        val arbeidssoekerIdSet = eksisterendeKafkaKeyRows
            .map { it.arbeidssoekerId }
            .toSet()

        val gjeldendeArbeidssoekerId = uledGjeldendeArbeidssoekerId(
            aktorId = aktorId,
            type = type,
            eksisterendeKafkaKeyRows = eksisterendeKafkaKeyRows
        )

        if (gjeldendeArbeidssoekerId != null) {
            // Slett identiteter som ikke var i PDL-hendelse
            eksisterendeIdentitetRows
                .filter { !konfliktIdentitetSet.contains(it.identitet) }
                .forEach { identitet ->
                    val rowsAffected = identitetRepository.updateByIdentitet(
                        identitet = identitet.identitet,
                        aktorId = aktorId,
                        gjeldende = false,
                        status = IdentitetStatus.SLETTET,
                        sourceTimestamp = identitet.sourceTimestamp
                    )
                    logger.info(
                        "Oppdaterer identitet av type {} med status {} (rows affected {})",
                        identitet.type.name,
                        IdentitetStatus.SLETTET.name,
                        rowsAffected
                    )
                }

            // Opprett eller oppdater alle identiteter fra PDL-hendelse
            konfliktIdentitetRows
                .forEach { identitet ->
                    if (eksisterendeIdentitetSet.contains(identitet.identitet)) {
                        val rowsAffected = identitetRepository.updateByIdentitet(
                            identitet = identitet.identitet,
                            aktorId = aktorId,
                            arbeidssoekerId = gjeldendeArbeidssoekerId,
                            gjeldende = identitet.gjeldende,
                            status = IdentitetStatus.AKTIV,
                            sourceTimestamp = sourceTimestamp
                        )
                        logger.info(
                            "Oppdaterer identitet av type {} med status {} (rows affected {})",
                            identitet.type.name,
                            IdentitetStatus.AKTIV.name,
                            rowsAffected
                        )
                    } else {
                        val rowsAffected = identitetRepository.insert(
                            arbeidssoekerId = gjeldendeArbeidssoekerId,
                            aktorId = aktorId,
                            identitet = identitet.identitet,
                            type = identitet.type,
                            gjeldende = identitet.gjeldende,
                            status = IdentitetStatus.AKTIV,
                            sourceTimestamp = sourceTimestamp
                        )
                        logger.info(
                            "Oppretter identitet av type {} med arbeidssoekerId og status {} (rows affected {})",
                            identitet.type.name,
                            IdentitetStatus.AKTIV.name,
                            rowsAffected
                        )
                    }
                }

            val nyeIdentiteter = identitetRepository.findByAktorId(aktorId)
                .filter { it.status != IdentitetStatus.SLETTET }
                .map { it.asIdentitet() }
                .toList()

            // Send hendelser for merge av identiteter
            arbeidssoekerIdSet
                .sorted()
                .forEach { arbeidssoekerId ->
                    val tidligereIdentiteter = eksisterendeIdentitetRows
                        .filter { it.arbeidssoekerId == arbeidssoekerId }
                        .filter { it.status != IdentitetStatus.SLETTET }
                        .map { it.asIdentitet() }
                        .toList()

                    val identiteter = if (arbeidssoekerId == gjeldendeArbeidssoekerId) {
                        nyeIdentiteter + arbeidssoekerIdSet
                            .sorted()
                            .map { it.asIdentitet(gjeldende = (it == gjeldendeArbeidssoekerId)) }
                            .toList()
                    } else {
                        emptyList()
                    }

                    hendelseService.sendIdentiteterMergetHendelse(
                        arbeidssoekerId = arbeidssoekerId,
                        identiteter = identiteter,
                        tidligereIdentiteter = tidligereIdentiteter + listOf(arbeidssoekerId.asIdentitet(gjeldende = true))
                    )

                    if (arbeidssoekerId != gjeldendeArbeidssoekerId) {
                        val identitetSet = tidligereIdentiteter
                            .map { it.identitet }
                            .toSet() + eksisterendeKafkaKeyRows
                            .filter { it.arbeidssoekerId == arbeidssoekerId }
                            .map { it.identitetsnummer }
                            .toSet()
                        val tidligereIdent = eksisterendeKafkaKeyRows
                            .filter { it.arbeidssoekerId == arbeidssoekerId }
                            .map { it.identitetsnummer }.minByOrNull { it.length }!!
                        val nyIdent = nyeIdentiteter
                            .filter { it.type == IdentitetType.FOLKEREGISTERIDENT }
                            .filter { it.gjeldende }
                            .map { it.identitet }
                            .first()
                        hendelseService.sendIdentitetsnummerSammenslaattHendelse(
                            fraArbeidssoekerId = arbeidssoekerId,
                            tilArbeidssoekerId = gjeldendeArbeidssoekerId,
                            identitet = tidligereIdent,
                            identiteter = identitetSet,
                            sourceTimestamp = sourceTimestamp
                        )
                        hendelseService.sendArbeidssoekerIdFlettetInnHendelse(
                            fraArbeidssoekerId = arbeidssoekerId,
                            tilArbeidssoekerId = gjeldendeArbeidssoekerId,
                            identitet = nyIdent,
                            identiteter = identitetSet,
                            sourceTimestamp = sourceTimestamp
                        )
                    }
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

    private fun uledGjeldendeArbeidssoekerId(
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
                .maxBy {
                    it.avsluttetTimestamp?.toEpochMilli() ?: it.startetTimestamp.toEpochMilli()
                }
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
                "Kunne ikke løse merge for identiteter på grunn av flere aktive perioder på forskjellige identiteter (rows affected {})",
                rowsAffected
            )
            null
        }
    }
}