package no.nav.paw.kafkakeygenerator.api.recordkey

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.*
import no.nav.paw.kafkakeygenerator.api.recordkey.functions.recordKey
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

fun Routing.configureRecordKeyApi(
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
        ?.let(::CallId)
        ?: CallId(UUID.randomUUID().toString())
    val identitetsnummer = Identitetsnummer(call.receive<RecordKeyLookupRequestV1>().ident)
    val (status, response) = applikasjon::hent.recordKey(logger, callId, identitetsnummer)
    call.respond(status, response)
}