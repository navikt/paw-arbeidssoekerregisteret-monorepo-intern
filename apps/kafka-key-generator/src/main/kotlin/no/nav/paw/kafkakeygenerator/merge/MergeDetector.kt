package no.nav.paw.kafkakeygenerator.merge

import no.nav.paw.kafkakeygenerator.*
import no.nav.paw.kafkakeygenerator.mergedetector.vo.MergeDetected
import no.nav.paw.kafkakeygenerator.pdl.PdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import org.slf4j.LoggerFactory

class MergeDetector(
    private val pdlIdentitesTjeneste: PdlIdentitesTjeneste,
    private val kafkaKeys: KafkaKeys
) {
    private val logger = LoggerFactory.getLogger("MergeDetector")

    suspend fun findMerges(batchSize: Int): Either<Failure, Long> {
        require(batchSize > 0) { "Batch size must be greater than 0" }
        return kafkaKeys.hentSisteArbeidssoekerId()
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
        logger.info("Processing range:stopAt={}, maxSize={}, currentPos={}, results={}", stopAt, maxSize, currentPos, results)
        return when (results) {
            is Left -> {
                return results
            }

            is Right -> {
                if (currentPos >= stopAt) {
                    results
                } else {
                    val storedData = kafkaKeys.hent(currentPos, maxSize)
                    val detected = storedData
                        .suspendingFlatMap {
                            pdlIdentitesTjeneste.hentIdenter(it.keys.toList()).map { res -> it to res }
                        }
                        .map { (local, pdl) -> detectMerges(local, pdl) }
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
    local: Map<Identitetsnummer, ArbeidssoekerId>,
    pdl: Map<String, List<IdentInformasjon>>
): Sequence<MergeDetected> {
    return pdl.asSequence()
        .mapNotNull { (searchedId, resultIds) ->
            val arbIds = resultIds
                .map { Identitetsnummer(it.ident) }
                .mapNotNull { pdlId ->
                    local[pdlId]?.let { pdlId to it }
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







