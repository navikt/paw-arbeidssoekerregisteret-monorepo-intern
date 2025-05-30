package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.exception.IdentitetIkkeFunnetException
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KafkaKeyRow
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysIdentitetRepository
import no.nav.paw.logging.logger.buildLogger
import java.time.Instant

class IdentitetService(
    private val identitetRepository: IdentitetRepository,
    private val identitetKonfliktService: IdentitetKonfliktService,
    private val identitetHendelseService: IdentitetHendelseService,
    private val kafkaKeysIdentitetRepository: KafkaKeysIdentitetRepository
) {
    private val logger = buildLogger

    fun identiteterSkalOppdateres(
        identiteter: List<Identitet>
    ) {
        // TODO: Vil feile om person ikke har aktør-id, men det skal vel ikke kunne skje?
        val aktorId = identiteter.first { it.type == IdentitetType.AKTORID }.identitet
        identiteterSkalOppdateres(
            aktorId = aktorId,
            identiteter = identiteter,
            sourceTimestamp = Instant.now()
        )
    }

    fun identiteterSkalOppdateres(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant
    ) {
        val identitetSet = identiteter
            .map { it.identitet }
            .toSet()

        val eksisterendeKafkaKeyRows = kafkaKeysIdentitetRepository
            .findByIdentiteter(identitetSet)
        val arbeidssoekerIdSet = eksisterendeKafkaKeyRows
            .map { it.arbeidssoekerId }
            .toSet()

        if (arbeidssoekerIdSet.isEmpty()) {
            logger.info(
                "Ignorer aktor-melding fordi person ikke er arbeidssøker ({} identiteter)",
                identiteter.size
            )
        } else if (arbeidssoekerIdSet.size > 1) {
            logger.warn(
                "Pauser aktor-melding fordi arbeidssøker har flere arbeidssøker-ider ({} identiteter)",
                identiteter.size
            )
            identiteterMedKonflikt(
                aktorId = aktorId,
                identiteter = identiteter,
                sourceTimestamp = sourceTimestamp,
                eksisterendeKafkaKeyRows = eksisterendeKafkaKeyRows
            )
        } else {
            logger.info("Endrer identiteter for arbeidssøker ({} identiteter)", identiteter.size)
            val arbeidssoekerId = arbeidssoekerIdSet.first()
            identiteterSkalEndres(
                aktorId = aktorId,
                identiteter = identiteter,
                sourceTimestamp = sourceTimestamp,
                arbeidssoekerId = arbeidssoekerId
            )
        }
    }

    fun identiteterSkalSlettes(aktorId: String) {
        val eksisterendeIdentitetRows = identitetRepository
            .findByAktorId(aktorId)

        if (eksisterendeIdentitetRows.isEmpty()) {
            logger.info("Ignorer tombstone-melding fordi ingen lagrede identiteter funnet")
        } else {
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
    }

    fun identiteterMedKonflikt(
        aktorId: String,
        identiteter: List<Identitet>,
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

        identiteter
            .map { identitet ->
                if (identiteterSet.contains(identitet.identitet)) {
                    val rowsAffected = identitetRepository.updateGjeldendeAndStatusByIdentitet(
                        identitet = identitet.identitet,
                        gjeldende = identitet.gjeldende,
                        status = IdentitetStatus.KONFLIKT
                    )
                    logger.info(
                        "Oppdaterer identitet av type {} med status {} (rows affected {})",
                        identitet.type.name,
                        IdentitetStatus.KONFLIKT.name,
                        rowsAffected
                    )
                } else {
                    val arbeidssoekerId = kafkaKeyMap[identitet.identitet] ?: nyesteArbeidssoekerId
                    val rowsAffected = identitetRepository.insert(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId,
                        identitet = identitet.identitet,
                        type = identitet.type,
                        gjeldende = identitet.gjeldende,
                        status = IdentitetStatus.KONFLIKT,
                        sourceTimestamp = sourceTimestamp
                    )
                    logger.info(
                        "Lagret identitet av type {} med status {} (rows affected {})",
                        identitet.type.name,
                        IdentitetStatus.KONFLIKT.name,
                        rowsAffected
                    )
                }
            }

        identitetKonfliktService.lagreVentendeIdentitetKonflikt(aktorId)
    }

    fun identiteterSkalEndres(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        arbeidssoekerId: Long
    ) {
        val eksisterendeIdentitetRows = identitetRepository
            .findByAktorId(aktorId)
        val identiteterMap = eksisterendeIdentitetRows
            .associate { it.identitet to it.gjeldende }

        val endredeIdentiteter = identiteter
            .map { identitet ->
                val erGjeldende = identiteterMap[identitet.identitet]
                if (erGjeldende != null) {
                    if (erGjeldende != identitet.gjeldende) {
                        val rowsAffected = identitetRepository.updateGjeldendeByIdentitet(
                            identitet = identitet.identitet,
                            gjeldende = identitet.gjeldende
                        )
                        logger.info(
                            "Oppdaterer identitet av type {} (rows affected {})",
                            identitet.type.name,
                            rowsAffected
                        )
                    }
                } else {
                    val rowsAffected = identitetRepository.insert(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId,
                        identitet = identitet.identitet,
                        type = identitet.type,
                        gjeldende = identitet.gjeldende,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = sourceTimestamp
                    )
                    logger.info(
                        "Lagret identitet av type {} med status {} (rows affected {})",
                        identitet.type.name,
                        IdentitetStatus.AKTIV.name,
                        rowsAffected
                    )
                }
                identitetRepository.getByIdentitet(identitet.identitet)
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