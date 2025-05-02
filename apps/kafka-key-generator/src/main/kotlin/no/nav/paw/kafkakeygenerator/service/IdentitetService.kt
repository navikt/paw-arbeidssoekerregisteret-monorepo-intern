package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.kafkakeygenerator.exception.IdentitetIkkeFunnetException
import no.nav.paw.kafkakeygenerator.model.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KafkaKeyRow
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.model.asIdentitetType
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.logging.logger.buildLogger
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import java.time.Instant

class IdentitetService(
    private val identitetRepository: IdentitetRepository,
    private val identitetKonfliktService: IdentitetKonfliktService,
    private val identitetHendelseService: IdentitetHendelseService
) {
    private val logger = buildLogger

    fun hentIdentiteterForAktorId(aktorId: String): List<IdentitetRow> {
        return identitetRepository
            .findByAktorId(aktorId)
    }

    fun identiteterSkalSlettes(
        aktorId: String,
        eksisterendeIdentitetRows: List<IdentitetRow>,
    ) {
        val rowsAffected = identitetRepository.updateStatusByAktorId(aktorId, IdentitetStatus.SLETTET)
        logger.info("Mottok tombstone-melding, soft-slettet {} tilhørende identiteter", rowsAffected)

        val arbeidssoekerIdList = eksisterendeIdentitetRows
            .map { it.arbeidssoekerId }
            .toSet()

        if (arbeidssoekerIdList.size == 1) {
            val arbeidssoekerId = arbeidssoekerIdList.first()
            val historiskeIdentiteter = eksisterendeIdentitetRows
                .map { it.asIdentitet() }
                .toMutableList()

            identitetHendelseService.lagreVentendeIdentitetHendelse(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId,
                identiteter = mutableListOf(),
                historiskeIdentiteter = historiskeIdentiteter
            )
        } else {
            identitetKonfliktService.lagreVentendeIdentitetKonflikt(aktorId)
        }
    }

    fun identiteterMedKonflikt(
        aktorId: String,
        aktor: Aktor,
        sourceTimestamp: Instant,
        eksisterendeKafkaKeyRows: List<KafkaKeyRow>
    ) {
        val identitetRows = identitetRepository
            .findByAktorId(aktorId)
        val identiteterSet = identitetRows
            .map { it.identitet }
            .toSet()
        val sisteArbeidssoekerId = eksisterendeKafkaKeyRows.maxOf { it.arbeidssoekerId }
        val kafkaKeyMap = eksisterendeKafkaKeyRows
            .associate { it.identitetsnummer to it.arbeidssoekerId }

        aktor.identifikatorer
            .map { identifikator ->
                if (identiteterSet.contains(identifikator.idnummer)) {
                    updateIdentitet(
                        identifikator = identifikator,
                        status = IdentitetStatus.KONFLIKT
                    )
                } else {
                    val arbeidssoekerId = kafkaKeyMap[identifikator.idnummer] ?: sisteArbeidssoekerId
                    insertIdentitet(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId,
                        identifikator = identifikator,
                        status = IdentitetStatus.KONFLIKT,
                        sourceTimestamp = sourceTimestamp
                    )
                }
            }

        identitetKonfliktService.lagreVentendeIdentitetKonflikt(aktorId)
    }

    fun identiteterSkalEndres(
        aktorId: String,
        aktor: Aktor,
        sourceTimestamp: Instant,
        arbeidssoekerId: Long
    ) {
        val eksisterendeIdentitetRows = identitetRepository
            .findByAktorId(aktorId)
        val identiteterSet = eksisterendeIdentitetRows
            .map { it.identitet }
            .toSet()

        val endredeIdentiteter = aktor.identifikatorer
            .map { identifikator ->
                if (identiteterSet.contains(identifikator.idnummer)) {
                    updateIdentitet(
                        identifikator = identifikator
                    )
                } else {
                    insertIdentitet(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId,
                        identifikator = identifikator,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = sourceTimestamp
                    )
                }
                identitetRepository.getByIdentitet(identifikator.idnummer)
                    ?.asIdentitet() ?: throw IdentitetIkkeFunnetException("Identitet ikke funnet")
            }
            .toMutableList()

        val historiskeIdentiteter = eksisterendeIdentitetRows
            .map { it.asIdentitet() }
            .toMutableList()

        identitetHendelseService.lagreVentendeIdentitetHendelse(
            arbeidssoekerId = arbeidssoekerId,
            aktorId = aktorId,
            identiteter = endredeIdentiteter,
            historiskeIdentiteter = historiskeIdentiteter
        )
    }

    private fun insertIdentitet(
        arbeidssoekerId: Long,
        aktorId: String,
        identifikator: Identifikator,
        status: IdentitetStatus,
        sourceTimestamp: Instant
    ) {
        val rowsAffected = identitetRepository.insert(
            arbeidssoekerId = arbeidssoekerId,
            aktorId = aktorId,
            identitet = identifikator.idnummer,
            type = identifikator.type.asIdentitetType(),
            gjeldende = identifikator.gjeldende,
            status = status,
            sourceTimestamp = sourceTimestamp
        )
        logger.info(
            "Lagret identitet av type {} med status {} (rows affected {})",
            identifikator.type.name,
            status.name,
            rowsAffected
        )
    }

    private fun updateIdentitet(
        identifikator: Identifikator,
        status: IdentitetStatus? = null,
    ) {
        if (status == null) {
            val rowsAffected = identitetRepository.updateGjeldendeByIdentitet(
                identitet = identifikator.idnummer,
                gjeldende = identifikator.gjeldende
            )
            logger.info(
                "Oppdaterer identitet av type {} (rows affected {})",
                identifikator.type.name,
                rowsAffected
            )
        } else {
            val rowsAffected = identitetRepository.updateGjeldendeAndStatusByIdentitet(
                identitet = identifikator.idnummer,
                gjeldende = identifikator.gjeldende,
                status = status
            )
            logger.info(
                "Oppdaterer identitet av type {} med status {} (rows affected {})",
                identifikator.type.name,
                status.name,
                rowsAffected
            )
        }
    }
}