package no.nav.paw.kafkakeygenerator.api.v1

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.api.v2.publicTopicKeyFunction
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.CallId
import no.nav.paw.kafkakeygenerator.model.Either
import no.nav.paw.kafkakeygenerator.model.Failure
import no.nav.paw.kafkakeygenerator.model.FailureCode
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.utils.getCallId
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Routing.apiV1Routes(
    kafkaKeysService: KafkaKeysService
) {
    val logger = LoggerFactory.getLogger("record-key-api")
    route("/api/v1") {
        autentisering(AzureAd) {
            post("/record-key") {
                handleRequest(kafkaKeysService, logger)
            }
        }
    }
}

@WithSpan
private suspend fun RoutingContext.handleRequest(
    kafkaKeysService: KafkaKeysService,
    logger: Logger
) {
    val callId = call.request.getCallId
    val identitetsnummer = Identitetsnummer(call.receive<RecordKeyLookupRequestV1>().ident)
    val (status, response) = kafkaKeysService::hentEllerOppdater.recordKey(logger, callId, identitetsnummer)
    call.respond(status, response)
}

suspend fun (suspend (CallId, Identitetsnummer) -> Either<Failure, ArbeidssoekerId>).recordKey(
    logger: Logger,
    callId: CallId,
    ident: Identitetsnummer
): Pair<HttpStatusCode, RecordKeyResponse> =
    invoke(callId, ident)
        .map(::publicTopicKeyFunction)
        .map(::recordKeyLookupResponseV1)
        .onLeft { failure ->
            if (failure.code() in listOf(FailureCode.INTERNAL_TECHINCAL_ERROR, FailureCode.CONFLICT)) {
                logger.error("kode: '{}', system: '{}'", failure.code(), failure.system(), failure.exception())
            } else {
                logger.debug("kode: '{}', system: '{}'", failure.code(), failure.system(), failure.exception())
            }
        }
        .map { right -> HttpStatusCode.OK to right }
        .fold(
            { mapFailure(it) },
            { it }
        )

private fun mapFailure(result: Failure) =
    when (result.code()) {
        FailureCode.PDL_NOT_FOUND ->
            HttpStatusCode.NotFound to
                    FailureResponseV1("Ukjent ident", Feilkode.UKJENT_IDENT)

        FailureCode.DB_NOT_FOUND -> HttpStatusCode.NotFound to
                FailureResponseV1("Ikke funnet i arbeidssøkerregisteret", Feilkode.UKJENT_REGISTERET)

        FailureCode.EXTERNAL_TECHINCAL_ERROR ->
            HttpStatusCode.InternalServerError to FailureResponseV1(
                "Teknisk feil ved kommunikasjon med eksternt system",
                Feilkode.TEKNISK_FEIL
            )


        FailureCode.INTERNAL_TECHINCAL_ERROR ->
            HttpStatusCode.InternalServerError to
                    FailureResponseV1("Intern feil", Feilkode.TEKNISK_FEIL)

        FailureCode.CONFLICT ->
            HttpStatusCode.InternalServerError to
                    FailureResponseV1("Intern feil", Feilkode.TEKNISK_FEIL)
    }