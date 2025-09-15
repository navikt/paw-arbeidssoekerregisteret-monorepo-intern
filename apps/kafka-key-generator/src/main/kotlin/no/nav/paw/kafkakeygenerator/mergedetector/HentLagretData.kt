package no.nav.paw.kafkakeygenerator.mergedetector

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.mergedetector.vo.LagretData
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.Either
import no.nav.paw.kafkakeygenerator.model.Failure
import no.nav.paw.kafkakeygenerator.model.FailureCode.DB_NOT_FOUND
import no.nav.paw.kafkakeygenerator.model.FailureCode.PDL_NOT_FOUND
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.model.Info
import no.nav.paw.kafkakeygenerator.model.PdlId
import no.nav.paw.kafkakeygenerator.model.flatten
import no.nav.paw.kafkakeygenerator.model.left
import no.nav.paw.kafkakeygenerator.model.recover
import no.nav.paw.kafkakeygenerator.model.right

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
        .map {
            LagretData(
                identitetsnummer = Identitetsnummer(info.identitetsnummer),
                lagretData = it
            )
        }
}
