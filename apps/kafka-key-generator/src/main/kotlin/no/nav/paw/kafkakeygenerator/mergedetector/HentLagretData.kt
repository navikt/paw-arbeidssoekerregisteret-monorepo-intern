package no.nav.paw.kafkakeygenerator.mergedetector

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.vo.FailureCode.DB_NOT_FOUND
import no.nav.paw.kafkakeygenerator.vo.FailureCode.PDL_NOT_FOUND
import no.nav.paw.kafkakeygenerator.vo.Info
import no.nav.paw.kafkakeygenerator.vo.PdlId
import no.nav.paw.kafkakeygenerator.mergedetector.vo.LagretData
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.flatten
import no.nav.paw.kafkakeygenerator.vo.left
import no.nav.paw.kafkakeygenerator.vo.recover
import no.nav.paw.kafkakeygenerator.vo.right

@WithSpan
fun hentLagretData(
    hentArbeidssoekerId: (Identitetsnummer) -> Either<Failure, ArbeidssoekerId>,
    info: Info
): Either<Failure, LagretData> {
    if (info.lagretData == null) return left(failure(DB_NOT_FOUND)) // Er ikke noe lagret kan vi ikke ha en merge
    val pdlData = info.pdlData.id
    if (pdlData.isNullOrEmpty()) return left(failure(PDL_NOT_FOUND)) // Er ikke noe pdl data kan vi ikke ha en merge
    return pdlData
        .asSequence()
        .filter { it.gruppe == "FOLKEREGISTERIDENT" }
        .map(PdlId::id)
        .map(::Identitetsnummer)
        .map { identitetsnummer ->
            hentArbeidssoekerId(identitetsnummer)
                .recover(DB_NOT_FOUND) { right(null) }
                .map { identitetsnummer to it }
        }
        .toList()
        .flatten()
        .map { it.toMap() }
        .map { LagretData(
            identitetsnummer = Identitetsnummer(info.identitetsnummer),
            lagretData = it
        ) }
}
