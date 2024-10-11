package no.nav.paw.kafkakeygenerator.api.oppslag

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.FailureCode
import no.nav.paw.kafkakeygenerator.Left
import no.nav.paw.kafkakeygenerator.Right
import no.nav.paw.kafkakeygenerator.api.v2.publicTopicKeyFunction
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

fun Routing.konfigurerOppslagsAPI(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    applikasjon: Applikasjon
) {
    val logger = LoggerFactory.getLogger("record-key-api")
    authenticate(autentiseringKonfigurasjon.kafkaKeyApiAuthProvider) {
        post("/api/oppslag/v1/metadata") {
            handleRequest(applikasjon, logger)
        }
    }
}

@WithSpan
private suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest(
    applikasjon: Applikasjon,
    logger: Logger
) {
    val callId = call.request.headers["traceparent"]
        ?.let { CallId(it) }
        ?: CallId(UUID.randomUUID().toString())
    val request = call.receive<RecordKeyLookupRequestV1>()
    val result = applikasjon.hent(callId, Identitetsnummer(request.ident))
        .map(::publicTopicKeyFunction)
        .map(::recordKeyLookupResponseV1)
        .onLeft { failure ->
            if (failure.code in listOf(FailureCode.INTERNAL_TECHINCAL_ERROR, FailureCode.CONFLICT)) {
                logger.error("kode: '{}', system: '{}'", failure.code, failure.system, failure.exception)
            } else {
                logger.debug("kode: '{}', system: '{}'", failure.code, failure.system, failure.exception)
            }
        }
    when (result) {
        is Right -> call.respond(HttpStatusCode.OK, result.right)
        is Left -> when (result.left.code) {
            FailureCode.PDL_NOT_FOUND -> call.respond(
                HttpStatusCode.NotFound,
                FailureResponseV1("Ukjent ident", Feilkode.UKJENT_IDENT)
            )

            FailureCode.DB_NOT_FOUND -> call.respond(
                HttpStatusCode.NotFound,
                FailureResponseV1("Ikke funnet i arbeidssøkerregisteret", Feilkode.UKJENT_REGISTERET)
            )

            FailureCode.EXTERNAL_TECHINCAL_ERROR -> call.respond(
                HttpStatusCode.InternalServerError,
                FailureResponseV1("Teknisk feil ved kommunikasjon med eksternt system", Feilkode.TEKNISK_FEIL)
            )

            FailureCode.INTERNAL_TECHINCAL_ERROR -> call.respond(
                HttpStatusCode.InternalServerError,
                FailureResponseV1("Intern feil", Feilkode.TEKNISK_FEIL)
            )

            FailureCode.CONFLICT -> call.respond(
                HttpStatusCode.InternalServerError,
                FailureResponseV1("Intern feil", Feilkode.TEKNISK_FEIL)
            )
        }
    }
}