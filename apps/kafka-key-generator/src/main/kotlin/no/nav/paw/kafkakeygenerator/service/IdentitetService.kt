package no.nav.paw.kafkakeygenerator.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.identitet.internehendelser.vo.Identitet
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
        /* TODO Utkoblet
        val aktorId = identiteter
            .filter { it.type == IdentitetType.AKTORID }
            .first { it.gjeldende }
            .identitet
        identiteterSkalOppdateres(
            aktorId = aktorId,
            identiteter = identiteter,
            sourceTimestamp = Instant.now()
        )*/
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
        } else if (identiteter.size == eksisterendeIdentiteter.size && identiteter == eksisterendeIdentiteter) {
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

        if (eksisterendeIdentitetRows.isEmpty()) {
            logger.info("Ignorer tombstone-melding fordi ingen lagrede identiteter funnet")
        } else {
            val rowsAffected = identitetRepository.updateStatusByAktorId(
                aktorId = aktorId,
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
        }
    }

    @WithSpan
    fun identiteterSkalSplittes(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        eksisterendeIdentitetRows: Iterable<IdentitetRow>
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

    @WithSpan
    fun identiteterSkalMerges(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        eksisterendeIdentitetRows: Iterable<IdentitetRow>,
        eksisterendeKafkaKeyRows: Iterable<KafkaKeyRow>
    ) {
        val arbeidssoekerIdSet = eksisterendeIdentitetRows
            .map { it.arbeidssoekerId }
            .toMutableSet()
        arbeidssoekerIdSet += eksisterendeKafkaKeyRows
            .map { it.arbeidssoekerId }
            .toSet()
        val nyesteArbeidssoekerId = arbeidssoekerIdSet.maxOf { it }

        lagreIdentiteter(
            aktorId = aktorId,
            identiteter = identiteter,
            sourceTimestamp = sourceTimestamp,
            arbeidssoekerId = nyesteArbeidssoekerId,
            eksisterendeIdentitetRows = eksisterendeIdentitetRows,
            status = IdentitetStatus.MERGE
        )

        konfliktService.lagreVentendeKonflikt(
            aktorId = aktorId,
            type = KonfliktType.MERGE,
            sourceTimestamp = sourceTimestamp,
            identiteter = identiteter
        )
    }

    @WithSpan
    fun identiteterSkalEndres(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        arbeidssoekerId: Long,
        eksisterendeIdentitetRows: Iterable<IdentitetRow>
    ) {
        val endredeIdentiteter = lagreIdentiteter(
            aktorId = aktorId,
            identiteter = identiteter,
            sourceTimestamp = sourceTimestamp,
            arbeidssoekerId = arbeidssoekerId,
            eksisterendeIdentitetRows = eksisterendeIdentitetRows,
            status = IdentitetStatus.AKTIV
        )

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

    private fun lagreIdentiteter(
        aktorId: String,
        identiteter: List<Identitet>,
        sourceTimestamp: Instant,
        arbeidssoekerId: Long,
        eksisterendeIdentitetRows: Iterable<IdentitetRow>,
        status: IdentitetStatus
    ): List<Identitet> {
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

        return identiteter
            .map { identitet ->
                if (eksisterendeIdentitetSet.contains(identitet.identitet)) {
                    val rowsAffected = identitetRepository.updateByIdentitet(
                        identitet = identitet.identitet,
                        aktorId = aktorId,
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
                } else {
                    val rowsAffected = identitetRepository.insert(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId,
                        identitet = identitet.identitet,
                        type = identitet.type,
                        gjeldende = identitet.gjeldende,
                        status = status,
                        sourceTimestamp = sourceTimestamp
                    )
                    logger.info(
                        "Oppretter identitet av type {} med status {} (rows affected {})",
                        identitet.type.name,
                        status.name,
                        rowsAffected
                    )
                }

                identitetRepository.getByIdentitet(identitet.identitet)
                    ?.asIdentitet() ?: throw IdentitetIkkeFunnetException("Identitet ikke funnet")
            }
            .toMutableList()
            .apply { add(arbeidssoekerId.asIdentitet(gjeldende = true)) }
    }
}