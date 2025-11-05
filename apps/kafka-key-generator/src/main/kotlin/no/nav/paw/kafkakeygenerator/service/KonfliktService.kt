package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.model.dao.IdentiteterTable
import no.nav.paw.kafkakeygenerator.model.dao.KonfliktIdentiteterTable
import no.nav.paw.kafkakeygenerator.model.dao.KonflikterTable
import no.nav.paw.kafkakeygenerator.model.dao.PerioderTable
import no.nav.paw.kafkakeygenerator.model.dao.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.dao.KonfliktIdentitetRow
import no.nav.paw.kafkakeygenerator.model.dao.KonfliktRow
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.dto.asIdentitet
import no.nav.paw.logging.logger.buildLogger
import java.time.Instant

class KonfliktService(
    private val applicationConfig: ApplicationConfig,
    private val hendelseService: HendelseService
) {
    private val logger = buildLogger

    fun finnVentendeKonflikter(
        aktorId: String
    ): List<KonfliktRow> {
        return KonflikterTable.findByAktorIdAndStatus(
            aktorId = aktorId,
            status = KonfliktStatus.VENTER
        )
    }

    fun lagreVentendeKonflikt(
        aktorId: String,
        type: KonfliktType,
        sourceTimestamp: Instant,
        identiteter: List<Identitet>
    ) {
        val nyeIdentiteterSet = identiteter.map { it.identitet }.toSet()
        val identitetMergeRows = KonflikterTable.findByAktorIdAndType(aktorId, type)
        if (identitetMergeRows.isEmpty()) {
            val status = KonfliktStatus.VENTER
            val rowsAffected = KonflikterTable.insert(
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

            val deletedRowCount = identitetMergeRow.identiteter
                .map { it.identitet }
                .filter { !nyeIdentiteterSet.contains(it) }
                .sumOf { identitet ->
                    KonfliktIdentiteterTable
                        .deleteByKonfliktIdAndIdentitet(konfliktId, identitet)
                }

            val insertedRowCount = identiteter
                .filter { !eksisterendeIdeniteterSet.contains(it.identitet) }
                .forEach { identitet ->
                    KonfliktIdentiteterTable.insert(konfliktId, identitet)
                }

            val updatedRowCount = identiteter
                .filter { eksisterendeIdeniteterSet.contains(it.identitet) }
                .forEach { identitet ->
                    KonfliktIdentiteterTable
                        .updateByKonfliktIdAndIdentitet(konfliktId, identitet)
                }

            logger.info(
                "Oppdaterte konflikt type {} ({} rows inserted, {} rows updated, {} rows deleted)",
                type.name,
                insertedRowCount,
                updatedRowCount,
                deletedRowCount
            )
        }
    }

    private fun handleKonflikter(
        type: KonfliktType,
        status: KonfliktStatus,
        batchSize: Int,
        handleKonflikt: (KonfliktRow) -> Unit
    ) {
        val ventendeKonfliktRows = KonflikterTable.findByTypeAndStatus(
            type = type,
            status = status,
            rowCount = batchSize
        )
        if (ventendeKonfliktRows.isEmpty()) {
            logger.debug(
                "Fant ingen konflikter av type {} med status {}",
                type.name,
                status.name
            )
        } else {
            val konfliktIdList = KonflikterTable.updateStatusByIdListReturning(
                idList = ventendeKonfliktRows.map { it.id },
                fraStatus = status,
                tilStatus = KonfliktStatus.PROSESSERER
            )

            val konfliktRows = KonflikterTable.findByIdList(konfliktIdList)

            logger.info(
                "Starter prosessering av {}/{}/{} konflikter av type {}",
                konfliktRows.size,
                konfliktIdList.size,
                ventendeKonfliktRows.size,
                type.name
            )

            konfliktRows.forEach(handleKonflikt)
        }
    }

    fun handleVentendeMergeKonflikter() = handleKonflikter(
        type = KonfliktType.MERGE,
        status = KonfliktStatus.VENTER,
        batchSize = applicationConfig.identitetMergeKonfliktJob.batchSize,
        handleKonflikt = ::handleVentendeMergeKonflikt
    )

    private fun handleVentendeMergeKonflikt(konfliktRow: KonfliktRow) {
        logger.debug("Håndterer konflikt av type {}", KonfliktType.MERGE.name)

        val endredeIdentitetRows = konfliktRow.identiteter
        val endredeIdentitetSet = endredeIdentitetRows
            .map { it.identitet }
            .toSet()
        val eksisterendeIdentitetRows = IdentiteterTable.findByAktorIdOrIdentiteter(
            aktorId = konfliktRow.aktorId,
            identiteter = endredeIdentitetSet
        )
        val eksisterendeIdentitetSet = eksisterendeIdentitetRows
            .map { it.identitet }
            .toSet()
        val slettedeIdentitetSet = eksisterendeIdentitetRows
            .filter { it.status == IdentitetStatus.SLETTET }
            .map { it.identitet }
            .toMutableSet()
        val arbeidssoekerIdSet = eksisterendeIdentitetRows
            .map { it.arbeidssoekerId }
            .toSet()

        val gjeldendeArbeidssoekerId = uledGjeldendeArbeidssoekerId(
            aktorId = konfliktRow.aktorId,
            type = konfliktRow.type,
            eksisterendeIdentitetRows = eksisterendeIdentitetRows
        )

        if (gjeldendeArbeidssoekerId == null) {
            logger.warn("Avbryter prosessering av {}-konflikt", KonfliktType.MERGE.name)
        } else {
            // Slett identiteter som ikke var i PDL-hendelse
            eksisterendeIdentitetRows
                .filter { !endredeIdentitetSet.contains(it.identitet) }
                .forEach { identitetRow ->
                    slettedeIdentitetSet.add(identitetRow.identitet)

                    slettIdentitet(
                        aktorId = konfliktRow.aktorId,
                        identitet = identitetRow.identitet,
                        type = identitetRow.type,
                        sourceTimestamp = konfliktRow.sourceTimestamp
                    )
                }

            // Opprett eller oppdater alle identiteter fra PDL-hendelse
            endredeIdentitetRows
                .forEach { identitet ->
                    if (eksisterendeIdentitetSet.contains(identitet.identitet)) {
                        oppdaterIdentitet(
                            aktorId = konfliktRow.aktorId,
                            arbeidssoekerId = gjeldendeArbeidssoekerId,
                            identitet = identitet,
                            sourceTimestamp = konfliktRow.sourceTimestamp
                        )
                    } else {
                        opprettIdentitet(
                            aktorId = konfliktRow.aktorId,
                            arbeidssoekerId = gjeldendeArbeidssoekerId,
                            identitet = identitet,
                            sourceTimestamp = konfliktRow.sourceTimestamp
                        )
                    }
                }

            val nyeIdentiteter = IdentiteterTable.findByAktorId(konfliktRow.aktorId)
                .filter { it.status != IdentitetStatus.SLETTET }
                .map { it.asIdentitet() }
                .toList()

            // Send hendelser for merge av identiteter
            arbeidssoekerIdSet
                .sorted()
                .forEach { arbeidssoekerId ->
                    val eksisterendeIdentiteter = eksisterendeIdentitetRows
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

                    val tidligereIdentiteter = eksisterendeIdentiteter + listOf(arbeidssoekerId.asIdentitet())

                    hendelseService.sendIdentiteterMergetHendelse(
                        arbeidssoekerId = arbeidssoekerId,
                        identiteter = identiteter.sortedBy { it.type.ordinal },
                        tidligereIdentiteter = tidligereIdentiteter.sortedBy { it.type.ordinal }
                    )

                    if (arbeidssoekerId != gjeldendeArbeidssoekerId) {
                        val tidligereIdentitetSet = eksisterendeIdentiteter
                            .map { it.identitet }
                            .filter { !slettedeIdentitetSet.contains(it) }
                            .toSet()
                        val tidligereIdent = tidligereIdentitetSet
                            .minByOrNull { it.length }!!
                        val gjeldendeIdentitetSet = nyeIdentiteter
                            .filter { it.type == IdentitetType.FOLKEREGISTERIDENT }
                            .filter { it.gjeldende }
                            .map { it.identitet }
                            .first()
                        hendelseService.sendIdentitetsnummerSammenslaattHendelse(
                            fraArbeidssoekerId = arbeidssoekerId,
                            tilArbeidssoekerId = gjeldendeArbeidssoekerId,
                            identitet = tidligereIdent,
                            identiteter = tidligereIdentitetSet,
                            sourceTimestamp = konfliktRow.sourceTimestamp
                        )
                        hendelseService.sendArbeidssoekerIdFlettetInnHendelse(
                            fraArbeidssoekerId = arbeidssoekerId,
                            tilArbeidssoekerId = gjeldendeArbeidssoekerId,
                            identitet = gjeldendeIdentitetSet,
                            identiteter = tidligereIdentitetSet,
                            sourceTimestamp = konfliktRow.sourceTimestamp
                        )
                    }
                }

            oppdaterKonflikt(
                aktorId = konfliktRow.aktorId,
                type = konfliktRow.type,
                status = KonfliktStatus.FULLFOERT
            )
        }
    }

    fun handleValgteSplittKonflikter() = handleKonflikter(
        type = KonfliktType.SPLITT,
        status = KonfliktStatus.VALGT,
        batchSize = applicationConfig.identitetSplittKonfliktJob.batchSize,
        handleKonflikt = ::handleValgtSplittKonflikt
    )

    fun handleValgtSplittKonflikt(konfliktRow: KonfliktRow) {
        logger.debug("Håndterer konflikt av type {}", konfliktRow.type.name)

        val nyeIdentitetSet = konfliktRow.identiteter
            .map { it.identitet }
            .toSet()
        val eksisterendeIdentitetRows = IdentiteterTable.findByAktorIdOrIdentiteter(
            aktorId = konfliktRow.aktorId,
            identiteter = nyeIdentitetSet
        )
        val arbeidssoekerIdSet = eksisterendeIdentitetRows
            .map { it.arbeidssoekerId }
            .toSet()
        if (arbeidssoekerIdSet.size != 1) {
            // Flere arbeidssøkerIder. Kan ikke håndtere merge.
            logger.error(
                "Kunne ikke løse {}-konflikt på grunn av funn av {} arbeidssøkerIder",
                konfliktRow.type.name,
                arbeidssoekerIdSet.size,
            )
            oppdaterKonflikt(
                aktorId = konfliktRow.aktorId,
                type = konfliktRow.type,
                status = KonfliktStatus.FEILET
            )
        } else {
            val arbeidssoekerId = arbeidssoekerIdSet.first()
            val eksisterendeIdentitetSet = eksisterendeIdentitetRows
                .map { it.identitet }
                .toSet()

            // Slett identiteter som ikke var i PDL-hendelse
            eksisterendeIdentitetRows
                .filter { it.status != IdentitetStatus.SLETTET }
                .filter { !nyeIdentitetSet.contains(it.identitet) }
                .forEach { identitetRow ->
                    slettIdentitet(
                        aktorId = konfliktRow.aktorId,
                        identitet = identitetRow.identitet,
                        type = identitetRow.type,
                        sourceTimestamp = konfliktRow.sourceTimestamp
                    )
                }

            // Opprett eller oppdater alle identiteter fra PDL-hendelse
            konfliktRow.identiteter.forEach { identitet ->
                if (eksisterendeIdentitetSet.contains(identitet.identitet)) {
                    oppdaterIdentitet(
                        aktorId = konfliktRow.aktorId,
                        arbeidssoekerId = arbeidssoekerId,
                        identitet = identitet,
                        sourceTimestamp = konfliktRow.sourceTimestamp
                    )
                } else {
                    opprettIdentitet(
                        aktorId = konfliktRow.aktorId,
                        arbeidssoekerId = arbeidssoekerId,
                        identitet = identitet,
                        sourceTimestamp = konfliktRow.sourceTimestamp
                    )
                }
            }

            val nyeIdentiteter = IdentiteterTable.findByAktorId(konfliktRow.aktorId)
                .filter { it.status != IdentitetStatus.SLETTET }
                .map { it.asIdentitet() }
                .toMutableList()
                .also {
                    if (it.isNotEmpty()) {
                        it.add(arbeidssoekerId.asIdentitet())
                    }
                }
            val tidligereIdentiteter = eksisterendeIdentitetRows
                .filter { it.status != IdentitetStatus.SLETTET }
                .filter { it.aktorId == konfliktRow.aktorId }
                .map { it.asIdentitet() }
                .toMutableList()
                .also {
                    if (it.isNotEmpty()) {
                        it.add(arbeidssoekerId.asIdentitet())
                    }
                }
            hendelseService.sendIdentiteterSplittetHendelse(
                arbeidssoekerId = arbeidssoekerId,
                identiteter = nyeIdentiteter,
                tidligereIdentiteter = tidligereIdentiteter
            )

            oppdaterKonflikt(
                aktorId = konfliktRow.aktorId,
                type = konfliktRow.type,
                status = KonfliktStatus.FULLFOERT
            )
        }
    }

    private fun uledGjeldendeArbeidssoekerId(
        aktorId: String,
        type: KonfliktType,
        eksisterendeIdentitetRows: List<IdentitetRow>
    ): Long? {
        val identiteter = eksisterendeIdentitetRows
            .map { it.identitet }
        val periodeRows = PerioderTable.findByIdentiteter(
            identitetList = identiteter
        )
        val aktivePeriodeRows = periodeRows.filter { it.avsluttetTimestamp == null }

        return if (periodeRows.isEmpty()) {
            // Ingen perioder. Velger nyeste arbeidssoekerId.
            eksisterendeIdentitetRows.maxOf { it.arbeidssoekerId }
        } else if (aktivePeriodeRows.isEmpty()) {
            // Ingen aktive perioder. Velger arbeidssoekerId tilhørende nyeste periode.
            val nyestePeriode = periodeRows
                .maxBy {
                    it.avsluttetTimestamp?.toEpochMilli() ?: it.startetTimestamp.toEpochMilli()
                }
            val valgtIdentitet = eksisterendeIdentitetRows.find { it.identitet == nyestePeriode.identitet }
                ?: throw IllegalStateException("Fant ikke identitet for periode")
            valgtIdentitet.arbeidssoekerId
        } else if (aktivePeriodeRows.size == 1) {
            // Én aktiv periode. Velger tilhørende arbeidssoekerId.
            val aktivPeriode = aktivePeriodeRows.first()
            val valgtIdentitet = eksisterendeIdentitetRows.find { it.identitet == aktivPeriode.identitet }
                ?: throw IllegalStateException("Fant ikke identitet for periode")
            valgtIdentitet.arbeidssoekerId
        } else {
            // Flere aktive perioder. Kan ikke håndtere merge.
            val rowsAffected = KonflikterTable.updateStatusByAktorIdAndType(
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

    private fun oppdaterKonflikt(
        aktorId: String,
        type: KonfliktType,
        status: KonfliktStatus
    ) {
        val identitetKonfliktRowsAffected = KonflikterTable.updateStatusByAktorIdAndType(
            aktorId = aktorId,
            type = type,
            status = status
        )
        logger.info(
            "Oppdaterer {}-konflikt med status {} (rows affected {})",
            type.name,
            status.name,
            identitetKonfliktRowsAffected
        )
    }

    private fun opprettIdentitet(
        aktorId: String,
        arbeidssoekerId: Long,
        identitet: KonfliktIdentitetRow,
        sourceTimestamp: Instant
    ) {
        val rowsAffected = IdentiteterTable.insert(
            arbeidssoekerId = arbeidssoekerId,
            aktorId = aktorId,
            identitet = identitet.identitet,
            type = identitet.type,
            gjeldende = identitet.gjeldende,
            status = IdentitetStatus.AKTIV,
            sourceTimestamp = sourceTimestamp
        )
        logger.info(
            "Oppretter identitet av type {} med status {} (rows affected {})",
            identitet.type.name,
            IdentitetStatus.AKTIV.name,
            rowsAffected
        )
    }

    private fun oppdaterIdentitet(
        aktorId: String,
        arbeidssoekerId: Long,
        identitet: KonfliktIdentitetRow,
        sourceTimestamp: Instant
    ) {
        val status = IdentitetStatus.AKTIV
        val rowsAffected = IdentiteterTable.updateByIdentitet(
            identitet = identitet.identitet,
            aktorId = aktorId,
            arbeidssoekerId = arbeidssoekerId,
            gjeldende = identitet.gjeldende,
            status = status,
            sourceTimestamp = sourceTimestamp
        )
        logger.info(
            "Oppdaterer identitet av type {} med status {} (rows affected {})",
            identitet.type.name,
            status.name,
            rowsAffected
        )
    }

    private fun slettIdentitet(
        aktorId: String,
        identitet: String,
        type: IdentitetType,
        sourceTimestamp: Instant
    ) {
        val status = IdentitetStatus.SLETTET
        val rowsAffected = IdentiteterTable.updateByIdentitet(
            identitet = identitet,
            aktorId = aktorId,
            gjeldende = false,
            status = status,
            sourceTimestamp = sourceTimestamp
        )
        logger.info(
            "Oppdaterer identitet av type {} med status {} (rows affected {})",
            type.name,
            status.name,
            rowsAffected
        )
    }

    fun handleMergeJobbFeilet(throwable: Throwable) {
        logger.error("Prosessering av konflikter av type ${KonfliktType.MERGE.name} feilet", throwable)
    }

    fun handleMergeJobbAvbrutt() {
        logger.warn("Prosessering av konflikter av type ${KonfliktType.MERGE.name} ble avbrutt")
    }

    fun handleSplittJobbFeilet(throwable: Throwable) {
        logger.error("Prosessering av konflikter av type ${KonfliktType.SPLITT.name} feilet", throwable)
    }

    fun handleSplittJobbAvbrutt() {
        logger.warn("Prosessering av konflikter av type ${KonfliktType.SPLITT.name} ble avbrutt")
    }
}