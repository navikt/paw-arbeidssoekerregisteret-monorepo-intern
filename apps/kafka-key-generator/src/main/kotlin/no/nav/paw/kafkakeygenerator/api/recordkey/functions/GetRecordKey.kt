package no.nav.paw.kafkakeygenerator.api.recordkey.functions

import io.ktor.http.*
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.api.recordkey.RecordKeyResponse
import no.nav.paw.kafkakeygenerator.api.recordkey.recordKeyLookupResponseV1
import no.nav.paw.kafkakeygenerator.api.v2.publicTopicKeyFunction
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.slf4j.Logger

suspend fun (suspend (CallId, Identitetsnummer) -> Either<Failure, ArbeidssoekerId>).recordKey(
    logger: Logger,
    callId: CallId,
    ident: Identitetsnummer
): Pair<HttpStatusCode, RecordKeyResponse> =
    invoke(callId, ident)
        .map(::publicTopicKeyFunction)
        .map(::recordKeyLookupResponseV1)
        .onLeft { failure ->
            if (failure.code in listOf(FailureCode.INTERNAL_TECHINCAL_ERROR, FailureCode.CONFLICT)) {
                logger.error("kode: '{}', system: '{}'", failure.code, failure.system, failure.exception)
            } else {
                logger.debug("kode: '{}', system: '{}'", failure.code, failure.system, failure.exception)
            }
        }
        .map { right -> HttpStatusCode.OK to right }
        .fold(
            { mapFailure(it) },
            { it }
        )