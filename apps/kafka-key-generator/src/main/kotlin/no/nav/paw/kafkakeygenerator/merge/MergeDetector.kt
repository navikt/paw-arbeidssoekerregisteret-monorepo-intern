package no.nav.paw.kafkakeygenerator.merge

import no.nav.paw.kafkakeygenerator.*
import no.nav.paw.kafkakeygenerator.mergedetector.vo.MergeDetected
import no.nav.paw.kafkakeygenerator.pdl.PdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

class MergeDetector(
    private val pdlIdentitesTjeneste: PdlIdentitesTjeneste,
    private val kafkaKeys: KafkaKeys
) {
    suspend fun findMerges(batchSize: Long): Either<Failure, List<MergeDetected>> {
        require(batchSize > 0) { "Batch size must be greater than 0" }
        return kafkaKeys.hentSisteArbeidssoekerId()
            .map { it.value }
            .map { (it - batchSize)..it }
            .suspendingFlatMap { processRange(it, right(emptyList())) }
    }

    tailrec suspend fun processRange(
        range: LongRange,
        results: Either<Failure, List<MergeDetected>>
    ): Either<Failure, List<MergeDetected>> {
        return when (results) {
            is Left -> {
                return results
            }

            is Right -> {
                if (range.isEmpty()) {
                    results
                } else {
                    val detected = kafkaKeys.hent(range)
                        .suspendingFlatMap {
                            pdlIdentitesTjeneste.hentIdenter(it.keys.toList()).map { res -> it to res }
                        }
                        .map { (local, pdl) ->
                            detectMerges(local, pdl)
                        }.map(results.right::plus)
                    val newStart = range.first - range.count()
                    val newEnd = range.first
                    processRange(newStart..<newEnd, detected)
                }
            }
        }
    }
}

fun detectMerges(
    local: Map<Identitetsnummer, ArbeidssoekerId>,
    pdl: Map<String, List<IdentInformasjon>>
): List<MergeDetected> {
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
        }.toList()
}

suspend fun <L, R, A> Either<L, R>.suspendingFlatMap(f: suspend (R) -> Either<L, A>): Either<L, A> =
    when (this) {
        is Right -> f(this.right)
        is Left -> this
    }






