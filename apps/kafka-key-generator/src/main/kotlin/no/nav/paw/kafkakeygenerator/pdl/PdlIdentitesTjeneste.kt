package no.nav.paw.kafkakeygenerator.pdl

import no.nav.paw.kafkakeygenerator.*
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.PdlException
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.pdl.hentIdenter

private const val consumerId = "paw-arbeidssoekerregisteret"
private const val behandlingsnummer = "B452"

class PdlIdentitesTjeneste(private val pdlKlient: PdlClient) {
    suspend fun hentIdentInformasjon(
        callId: CallId,
        identitet: Identitetsnummer
    ): Either<Failure, List<IdentInformasjon>> {
        return suspendeableAttempt {
            pdlKlient
                .hentIdenter(identitet.value, callId.value, consumerId, behandlingsnummer)
        }.mapToFailure { exception ->
            when (exception) {
                is PdlException -> mapPdlException(exception)
                else -> Failure("pdl", FailureCode.EXTERNAL_TECHINCAL_ERROR, exception)
            }
        }.flatMap { hits ->
            if (hits.isNullOrEmpty()) {
                left(Failure("pdl", FailureCode.PDL_NOT_FOUND))
            } else {
                right(hits)
            }
        }
    }

    suspend fun hentIdentiter(
        callId: CallId,
        identitet: Identitetsnummer
    ): Either<Failure, List<String>> = hentIdentInformasjon(callId, identitet)
        .map { liste -> liste.map { it.ident } }

    private fun mapPdlException(ex: PdlException): Failure {
        return if (ex.errors?.any { it.message.contains("Fant ikke person") } == true) {
            Failure("pdl", FailureCode.PDL_NOT_FOUND)
        } else {
            Failure("pdl", FailureCode.EXTERNAL_TECHINCAL_ERROR, ex)
        }
    }
}
