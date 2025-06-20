package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.exception.IdentitetIkkeFunnetException
import no.nav.paw.kafkakeygenerator.model.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KafkaKeyRow
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysIdentitetRepository
import no.nav.paw.logging.logger.buildLogger
import java.time.Instant

class IdentitetService(
    private val identitetRepository: IdentitetRepository,
    private val konfliktService: KonfliktService,
    private val hendelseService: HendelseService,
    private val kafkaKeysIdentitetRepository: KafkaKeysIdentitetRepository
) {
    private val logger = buildLogger

    fun identiteterSkalOppdateres(
        identiteter: List<Identitet>
    ) {
        // Vil feile om person ikke har aktiv aktør-id, men det skal vel ikke kunne skje?
        val aktorId = identiteter
            .filter { it.type == IdentitetType.AKTORID }
            .first { it.gjeldende }
            .identitet
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
        val eksisterendeIdentitetRows = identitetRepository
            .findByAktorIdOrIdentiteter(
                aktorId = aktorId,
                identiteter = identitetSet
            )
        val arbeidssoekerIdSet = eksisterendeIdentitetRows
            .map { it.arbeidssoekerId }
            .toMutableSet()
        val aktorIdSet = eksisterendeIdentitetRows
            .map { it.aktorId }
            .toSet()
        val eksisterendeKafkaKeyRows = kafkaKeysIdentitetRepository
            .findByIdentiteter(identiteter = identitetSet)
        arbeidssoekerIdSet += eksisterendeKafkaKeyRows
            .map { it.arbeidssoekerId }
            .toSet()

        if (arbeidssoekerIdSet.isEmpty()) {
            logger.info(
                "Ignorer aktor-melding fordi person ikke er arbeidssøker ({} identiteter)",
                identiteter.size
            )
        } else if (aktorIdSet.isNotEmpty() && !aktorIdSet.contains(aktorId)) {
            logger.warn(
                "Pauser aktor-melding som splitt fordi person har ny aktør-ID ({} identiteter)",
                identiteter.size
            )
            identiteterSkalSplittes(
                aktorId = aktorId,
                identiteter = identiteter,
                sourceTimestamp = sourceTimestamp,
                eksisterendeIdentitetRows = eksisterendeIdentitetRows
            )
        } else if (arbeidssoekerIdSet.size > 1) {
            logger.warn(
                "Pauser aktor-melding som merge fordi arbeidssøker har flere arbeidssøker-ider ({} identiteter)",
                identiteter.size
            )
            identiteterSkalMerges(
                aktorId = aktorId,
                identiteter = identiteter,
                sourceTimestamp = sourceTimestamp,
                eksisterendeIdentitetRows = eksisterendeIdentitetRows,
                eksisterendeKafkaKeyRows = eksisterendeKafkaKeyRows
            )
        } else {
            logger.info("Endrer identiteter for arbeidssøker ({} identiteter)", identiteter.size)
            identiteterSkalEndres(
                aktorId = aktorId,
                identiteter = identiteter,
                sourceTimestamp = sourceTimestamp,
                arbeidssoekerId = arbeidssoekerIdSet.first(),
                eksisterendeIdentitetRows = eksisterendeIdentitetRows
            )
        }
    }

    fun identiteterSkalSlettes(
        aktorId: String,
        sourceTimestamp: Instant
    ) {
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

                hendelseService.lagreIdentiteterEndretHendelse(
                    aktorId = aktorId,
                    arbeidssoekerId = arbeidssoekerId,
                    identiteter = emptyList(),
                    tidligereIdentiteter = tidligereIdentiteter
                )
            } else {
                konfliktService.lagreVentendeKonflikt(
                    aktorId = aktorId,
                    type = KonfliktType.SLETT,
                    sourceTimestamp = sourceTimestamp,
                    identiteter = emptyList()
                )
            }
        }
    }

    fun identiteterSkalSplittes(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        eksisterendeIdentitetRows: List<IdentitetRow>
    ) {
        val aktorIdSet = eksisterendeIdentitetRows
            .map { it.aktorId }
            .toSet()
        identitetRepository.updateStatusByAktorIdList(
            status = IdentitetStatus.SPLITT,
            aktorIdList = aktorIdSet
        )
        konfliktService.lagreVentendeKonflikt(
            aktorId = aktorId,
            type = KonfliktType.SPLITT,
            sourceTimestamp = sourceTimestamp,
            identiteter = identiteter,
        )
    }

    fun identiteterSkalMerges(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        eksisterendeIdentitetRows: List<IdentitetRow>,
        eksisterendeKafkaKeyRows: List<KafkaKeyRow>
    ) {
        val identiteterSet = eksisterendeIdentitetRows
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
                        status = IdentitetStatus.MERGE
                    )
                    logger.info(
                        "Oppdaterer identitet av type {} med status {} (rows affected {})",
                        identitet.type.name,
                        IdentitetStatus.MERGE.name,
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
                        status = IdentitetStatus.MERGE,
                        sourceTimestamp = sourceTimestamp
                    )
                    logger.info(
                        "Oppretter identitet av type {} med status {} (rows affected {})",
                        identitet.type.name,
                        IdentitetStatus.MERGE.name,
                        rowsAffected
                    )
                }
            }

        konfliktService.lagreVentendeKonflikt(
            aktorId = aktorId,
            type = KonfliktType.MERGE,
            sourceTimestamp = sourceTimestamp,
            identiteter = identiteter
        )
    }

    fun identiteterSkalEndres(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        arbeidssoekerId: Long,
        eksisterendeIdentitetRows: List<IdentitetRow>
    ) {
        val identitetSet = identiteter
            .map { it.identitet }
            .toSet()
        val eksisterendeIdentitetSet = eksisterendeIdentitetRows
            .map { it.identitet }
            .toSet()

        val endredeIdentiteter = identiteter
            .map { identitet ->
                if (eksisterendeIdentitetSet.contains(identitet.identitet)) {
                    val rowsAffected = identitetRepository.updateByIdentitet(
                        identitet = identitet.identitet,
                        aktorId = aktorId,
                        gjeldende = identitet.gjeldende
                    )
                    logger.info(
                        "Oppdaterer identitet av type {} (rows affected {})",
                        identitet.type.name,
                        rowsAffected
                    )
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
                        "Oppretter identitet av type {} med status {} (rows affected {})",
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

        val slettIdentiteter = eksisterendeIdentitetSet
            .filter { !identitetSet.contains(it) }
            .toSet()
        if (slettIdentiteter.isNotEmpty()) {
            val rowsAffected = identitetRepository.updateStatusByIdentitetList(
                status = IdentitetStatus.SLETTET,
                identitetList = slettIdentiteter
            )
            logger.info(
                "Oppdaterer identiteter med status {} (rows affected {})",
                IdentitetStatus.AKTIV.name,
                rowsAffected
            )
        }

        val tidligereIdentiteter = eksisterendeIdentitetRows.map { it.asIdentitet() }.toMutableList()
        if (tidligereIdentiteter.isNotEmpty()) {
            tidligereIdentiteter.add(arbeidssoekerId.asIdentitet(gjeldende = true))
        }

        hendelseService.lagreIdentiteterEndretHendelse(
            arbeidssoekerId = arbeidssoekerId,
            aktorId = aktorId,
            identiteter = endredeIdentiteter,
            tidligereIdentiteter = tidligereIdentiteter
        )
    }
}