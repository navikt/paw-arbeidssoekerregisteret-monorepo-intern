package no.nav.paw.kafkakeygenerator.pdl

import no.nav.paw.kafkakeygenerator.*
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.PdlException
import no.nav.paw.pdl.hentIdenter

private const val consumerId = "paw-arbeidssoekerregisteret"

class PdlIdentitesTjeneste(private val pdlKlient: PdlClient) {
    suspend fun hentIdentiter(
        callId: CallId,
        identitet: Identitetsnummer
    ): Either<Failure, List<String>> {
        return suspendeableAttempt {
            pdlKlient
                .hentIdenter(identitet.value, callId.value, consumerId)
                ?.map { it.ident }
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

    private fun mapPdlException(ex: PdlException): Failure {
        return if (ex.errors?.any { it.message.contains("Fant ikke person") } == true) {
            Failure("pdl", FailureCode.PDL_NOT_FOUND)
        } else {
            Failure("pdl", FailureCode.EXTERNAL_TECHINCAL_ERROR, ex)
        }
    }
}
