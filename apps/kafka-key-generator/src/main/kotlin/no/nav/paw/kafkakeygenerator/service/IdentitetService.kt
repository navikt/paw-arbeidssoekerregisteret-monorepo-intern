package no.nav.paw.kafkakeygenerator.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
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

    fun hent(identitet: String): Identitet? {
        return identitetRepository
            .getByIdentitet(identitet)
            ?.takeIf { it.status != IdentitetStatus.SLETTET }
            ?.asIdentitet()
    }

    fun finnForAktorId(aktorId: String): List<Identitet> {
        return identitetRepository
            .findByAktorId(aktorId)
            .filter { it.status != IdentitetStatus.SLETTET }
            .map { it.asIdentitet() }
    }

    fun finnForArbeidssoekerId(arbeidssoekerId: Long): List<Identitet> {
        return identitetRepository
            .findByArbeidssoekerId(arbeidssoekerId)
            .filter { it.status != IdentitetStatus.SLETTET }
            .map { it.asIdentitet() }
    }

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

    @WithSpan
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
            ).toSet()
        val eksisterendeIdentiteter = eksisterendeIdentitetRows
            .map { it.asIdentitet() }
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
        val eksisterendeKonfliktRows = konfliktService.finnVentendeKonflikter(
            aktorId = aktorId
        )

        if (arbeidssoekerIdSet.isEmpty()) {
            logger.info(
                "Ignorer aktor-melding fordi person ikke er arbeidssøker ({} identiteter)",
                identiteter.size
            )
        } else if ((aktorIdSet.isNotEmpty() && !aktorIdSet.contains(aktorId)) || eksisterendeKonfliktRows.any { it.type == KonfliktType.SPLITT }) {
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
        } else if (arbeidssoekerIdSet.size > 1 || eksisterendeKonfliktRows.any { it.type == KonfliktType.MERGE }) {
            logger.warn(
                "Pauser aktor-melding som merge fordi arbeidssøker har flere arbeidssøker-ider ({} identiteter)",
                identiteter.size
            )
            identiteterSkalMerges(
                aktorId = aktorId,
                identiteter = identiteter,
                sourceTimestamp = sourceTimestamp,
                eksisterendeIdentitetRows = eksisterendeIdentitetRows
            )
        } else if (identiteter.size == eksisterendeIdentiteter.size && identiteter.containsAll(eksisterendeIdentiteter)) {
            logger.info(
                "Ignorer aktor-melding fordi alle identiteter eksisterer allerede ({} identiteter)",
                identiteter.size
            )
        } else {
            logger.info("Håndterer endringer i identiteter for arbeidssøker ({} identiteter)", identiteter.size)
            identiteterSkalEndres(
                aktorId = aktorId,
                identiteter = identiteter,
                sourceTimestamp = sourceTimestamp,
                arbeidssoekerId = arbeidssoekerIdSet.first(),
                eksisterendeIdentitetRows = eksisterendeIdentitetRows
            )
        }
    }

    @WithSpan
    fun identiteterSkalSlettes(
        aktorId: String,
        sourceTimestamp: Instant
    ) {
        val eksisterendeIdentitetRows = identitetRepository
            .findByAktorId(aktorId)

        if (eksisterendeIdentitetRows.none { it.status != IdentitetStatus.SLETTET }) {
            logger.info("Ignorer tombstone-melding fordi ingen aktive identiteter funnet")
        } else {
            val eksisterendeKonfliktRows = konfliktService.finnVentendeKonflikter(
                aktorId = aktorId
            )

            if (eksisterendeKonfliktRows.isEmpty()) {
                val rowsAffected = identitetRepository.updateGjeldendeAndStatusByAktorId(
                    aktorId = aktorId,
                    gjeldende = false,
                    status = IdentitetStatus.SLETTET
                )
                logger.info("Mottok tombstone-melding, soft-slettet {} tilhørende identiteter", rowsAffected)

                val arbeidssoekerIdSet = eksisterendeIdentitetRows
                    .map { it.arbeidssoekerId }
                    .toSet()

                if (arbeidssoekerIdSet.size == 1) {
                    val arbeidssoekerId = arbeidssoekerIdSet.first()
                    val tidligereIdentiteter = eksisterendeIdentitetRows
                        .filter { it.status != IdentitetStatus.SLETTET }
                        .map { it.asIdentitet() }
                        .toMutableList()
                    if (tidligereIdentiteter.isNotEmpty()) {
                        tidligereIdentiteter += arbeidssoekerId.asIdentitet(gjeldende = true)
                    }

                    hendelseService.sendIdentiteterSlettetHendelse(
                        arbeidssoekerId = arbeidssoekerId,
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
            } else {
                logger.info(
                    "Mottok tombstone-melding, finnes allerede {} ventende konflikter så lagrer {}-konflikt",
                    eksisterendeKonfliktRows.size,
                    KonfliktType.SLETT.name
                )

                konfliktService.lagreVentendeKonflikt(
                    aktorId = aktorId,
                    type = KonfliktType.SLETT,
                    sourceTimestamp = sourceTimestamp,
                    identiteter = emptyList()
                )
            }
        }
    }

    @WithSpan
    private fun identiteterSkalSplittes(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        eksisterendeIdentitetRows: Iterable<IdentitetRow>
    ) {
        val aktorIdSet = eksisterendeIdentitetRows
            .map { it.aktorId }
            .toSet()
        identitetRepository.updateStatusByNotSlettetAndAktorIdList(
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

    @WithSpan
    private fun identiteterSkalMerges(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        eksisterendeIdentitetRows: Iterable<IdentitetRow>
    ) {
        val aktorIdSet = eksisterendeIdentitetRows
            .map { it.aktorId }
            .toSet()
        identitetRepository.updateStatusByNotSlettetAndAktorIdList(
            status = IdentitetStatus.MERGE,
            aktorIdList = aktorIdSet
        )
        konfliktService.lagreVentendeKonflikt(
            aktorId = aktorId,
            type = KonfliktType.MERGE,
            sourceTimestamp = sourceTimestamp,
            identiteter = identiteter,
        )
    }

    @WithSpan
    private fun identiteterSkalEndres(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        arbeidssoekerId: Long,
        eksisterendeIdentitetRows: Iterable<IdentitetRow>
    ) {
        val identitetSet = identiteter
            .map { it.identitet }
            .toSet()
        val eksisterendeIdentitetSet = eksisterendeIdentitetRows
            .map { it.identitet }
            .toSet()

        eksisterendeIdentitetRows
            .filter { !identitetSet.contains(it.identitet) }
            .forEach { identitet ->
                val rowsAffected = identitetRepository.updateByIdentitet(
                    identitet = identitet.identitet,
                    aktorId = aktorId,
                    gjeldende = false,
                    status = IdentitetStatus.SLETTET,
                    sourceTimestamp = sourceTimestamp
                )
                logger.info(
                    "Oppdaterer identitet av type {} med status {} (rows affected {})",
                    identitet.type.name,
                    IdentitetStatus.SLETTET.name,
                    rowsAffected
                )
            }

        identiteter
            .forEach { identitet ->
                if (eksisterendeIdentitetSet.contains(identitet.identitet)) {
                    val rowsAffected = identitetRepository.updateByIdentitet(
                        identitet = identitet.identitet,
                        aktorId = aktorId,
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
            }

        val endredeIdentiteter = identitetRepository.findByAktorId(aktorId)
            .filter { it.status != IdentitetStatus.SLETTET }
            .map { it.asIdentitet() }
            .toMutableList()
            .apply { add(arbeidssoekerId.asIdentitet(gjeldende = true)) }

        val tidligereIdentiteter = eksisterendeIdentitetRows
            .filter { it.aktorId == aktorId }
            .filter { it.status != IdentitetStatus.SLETTET }
            .map { it.asIdentitet() }
            .toMutableList()
        if (tidligereIdentiteter.isNotEmpty()) {
            tidligereIdentiteter += arbeidssoekerId.asIdentitet(gjeldende = true)
        }

        hendelseService.sendIdentiteterEndretHendelse(
            arbeidssoekerId = arbeidssoekerId,
            identiteter = endredeIdentiteter,
            tidligereIdentiteter = tidligereIdentiteter
        )
    }
}