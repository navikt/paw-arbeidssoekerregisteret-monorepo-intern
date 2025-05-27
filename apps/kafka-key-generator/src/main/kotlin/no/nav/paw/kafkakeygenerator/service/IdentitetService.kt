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
        val rowsAffected = identitetRepository.updateStatusByAktorId(
            aktorId = aktorId,
            status = IdentitetStatus.SLETTET
        )
        logger.info("Mottok tombstone-melding, soft-slettet {} tilhørende identiteter", rowsAffected)

        val arbeidssoekerIdList = eksisterendeIdentitetRows
            .map { it.arbeidssoekerId }
            .toSet()

        if (arbeidssoekerIdList.size == 1) {
            val arbeidssoekerId = arbeidssoekerIdList.first()
            val tidligereIdentiteter = eksisterendeIdentitetRows
                .map { it.asIdentitet() }
                .toMutableList()
                .apply { add(arbeidssoekerId.asIdentitet(gjeldende = true)) }

            identitetHendelseService.lagreIdentiteterEndretHendelse(
                aktorId = aktorId,
                arbeidssoekerId = arbeidssoekerId,
                identiteter = mutableListOf(),
                tidligereIdentiteter = tidligereIdentiteter
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
        val nyesteArbeidssoekerId = eksisterendeKafkaKeyRows.maxOf { it.arbeidssoekerId }
        val kafkaKeyMap = eksisterendeKafkaKeyRows
            .associate { it.identitetsnummer to it.arbeidssoekerId }

        aktor.identifikatorer
            .map { identifikator ->
                if (identiteterSet.contains(identifikator.idnummer)) {
                    val rowsAffected = identitetRepository.updateGjeldendeAndStatusByIdentitet(
                        identitet = identifikator.idnummer,
                        gjeldende = identifikator.gjeldende,
                        status = IdentitetStatus.KONFLIKT
                    )
                    logger.info(
                        "Oppdaterer identitet av type {} med status {} (rows affected {})",
                        identifikator.type.name,
                        IdentitetStatus.KONFLIKT.name,
                        rowsAffected
                    )
                } else {
                    val arbeidssoekerId = kafkaKeyMap[identifikator.idnummer] ?: nyesteArbeidssoekerId
                    val rowsAffected = identitetRepository.insert(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId,
                        identitet = identifikator.idnummer,
                        type = identifikator.type.asIdentitetType(),
                        gjeldende = identifikator.gjeldende,
                        status = IdentitetStatus.KONFLIKT,
                        sourceTimestamp = sourceTimestamp
                    )
                    logger.info(
                        "Lagret identitet av type {} med status {} (rows affected {})",
                        identifikator.type.name,
                        IdentitetStatus.KONFLIKT.name,
                        rowsAffected
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
        val identiteterMap = eksisterendeIdentitetRows
            .associate { it.identitet to it.gjeldende }

        val endredeIdentiteter = aktor.identifikatorer
            .map { identifikator ->
                val erGjeldende = identiteterMap[identifikator.idnummer]
                if (erGjeldende != null) {
                    if (erGjeldende != identifikator.gjeldende) {
                        val rowsAffected = identitetRepository.updateGjeldendeByIdentitet(
                            identitet = identifikator.idnummer,
                            gjeldende = identifikator.gjeldende
                        )
                        logger.info(
                            "Oppdaterer identitet av type {} (rows affected {})",
                            identifikator.type.name,
                            rowsAffected
                        )
                    }
                } else {
                    val rowsAffected = identitetRepository.insert(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId,
                        identitet = identifikator.idnummer,
                        type = identifikator.type.asIdentitetType(),
                        gjeldende = identifikator.gjeldende,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = sourceTimestamp
                    )
                    logger.info(
                        "Lagret identitet av type {} med status {} (rows affected {})",
                        identifikator.type.name,
                        IdentitetStatus.AKTIV.name,
                        rowsAffected
                    )
                }
                identitetRepository.getByIdentitet(identifikator.idnummer)
                    ?.asIdentitet() ?: throw IdentitetIkkeFunnetException("Identitet ikke funnet")
            }
            .toMutableList()
            .apply { add(arbeidssoekerId.asIdentitet(gjeldende = true)) }

        val tidligereIdentiteter = eksisterendeIdentitetRows
            .map { it.asIdentitet() }
            .toMutableList()
        if (tidligereIdentiteter.isNotEmpty()) {
            tidligereIdentiteter.add(arbeidssoekerId.asIdentitet(gjeldende = true))
        }

        identitetHendelseService.lagreIdentiteterEndretHendelse(
            arbeidssoekerId = arbeidssoekerId,
            aktorId = aktorId,
            identiteter = endredeIdentiteter,
            tidligereIdentiteter = tidligereIdentiteter
        )
    }
}