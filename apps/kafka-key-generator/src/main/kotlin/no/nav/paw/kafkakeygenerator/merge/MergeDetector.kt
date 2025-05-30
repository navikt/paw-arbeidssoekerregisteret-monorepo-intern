package no.nav.paw.kafkakeygenerator.merge

import no.nav.paw.kafkakeygenerator.mergedetector.vo.MergeDetected
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.service.PdlService
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.Left
import no.nav.paw.kafkakeygenerator.vo.Right
import no.nav.paw.kafkakeygenerator.vo.right
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import org.slf4j.LoggerFactory

class MergeDetector(
    private val pdlService: PdlService,
    private val kafkaKeysRepository: KafkaKeysRepository
) {
    private val logger = LoggerFactory.getLogger("MergeDetector")
    private val hentEllerNull: (Identitetsnummer) -> ArbeidssoekerId? = { id ->
        kafkaKeysRepository.hent(id)
            .fold(
                { null },
                { it }
            )
    }

    suspend fun findMerges(batchSize: Int): Either<Failure, Long> {
        require(batchSize > 0) { "Batch size must be greater than 0" }
        return kafkaKeysRepository.hentSisteArbeidssoekerId()
            .map { it.value }
            .suspendingFlatMap { max ->
                processRange(
                    stopAt = max,
                    maxSize = batchSize,
                    currentPos = 0L,
                    right(0L)
                )
            }
    }

    tailrec suspend fun processRange(
        stopAt: Long,
        maxSize: Int,
        currentPos: Long,
        results: Either<Failure, Long>
    ): Either<Failure, Long> {
        logger.info(
            "Processing range:stopAt={}, maxSize={}, currentPos={}, results={}",
            stopAt,
            maxSize,
            currentPos,
            results
        )
        return when (results) {
            is Left -> {
                return results
            }

            is Right -> {
                if (currentPos >= stopAt) {
                    results
                } else {
                    val storedData = kafkaKeysRepository.hent(currentPos, maxSize)
                    val detected = storedData
                        .suspendingFlatMap {
                            pdlService.hentIdenter(it.keys.toList())
                        }
                        .map { pdl -> detectMerges(hentEllerNull, pdl) }
                        .map(Sequence<MergeDetected>::count)
                        .map(Int::toLong)
                        .map(results.right::plus)
                    val newStart =
                        storedData.fold({ -1L }, { it.values.maxOfOrNull(ArbeidssoekerId::value)?.plus(1) ?: -1 })
                    processRange(stopAt, maxSize, newStart, detected)
                }
            }
        }
    }
}

fun detectMerges(
    local: (Identitetsnummer) -> ArbeidssoekerId?,
    pdl: Map<String, List<IdentInformasjon>>
): Sequence<MergeDetected> {
    return pdl.asSequence()
        .mapNotNull { (searchedId, resultIds) ->
            val arbIds = resultIds
                .map { Identitetsnummer(it.ident) }
                .mapNotNull { pdlId ->
                    local(pdlId)?.let { pdlId to it }
                }
            if (arbIds.map { (_, arbId) -> arbId }.distinct().size > 1) {
                MergeDetected(
                    id = Identitetsnummer(searchedId),
                    map = arbIds
                        .groupBy { (_, arbId) -> arbId }
                        .mapValues { (_, value) -> value.map { (pdlId, _) -> pdlId } }
                )
            } else {
                null
            }
        }
}

suspend fun <L, R, A> Either<L, R>.suspendingFlatMap(f: suspend (R) -> Either<L, A>): Either<L, A> =
    when (this) {
        is Right -> f(this.right)
        is Left -> this
    }







