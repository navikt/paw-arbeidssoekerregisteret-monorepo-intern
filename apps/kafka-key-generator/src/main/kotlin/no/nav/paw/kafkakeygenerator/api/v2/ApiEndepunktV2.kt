package no.nav.paw.kafkakeygenerator.api.v2

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.OK
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
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

fun Routing.konfigurerApiV2(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    applikasjon: Applikasjon
) {
    val logger = LoggerFactory.getLogger("api")
    authenticate(autentiseringKonfigurasjon.kafkaKeyApiAuthProvider) {
        post("/api/v2/hentEllerOpprett") {
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
    val request = call.receive<RequestV2>()
    when (val resultat = applikasjon.hentEllerOpprett(callId, Identitetsnummer(request.ident))) {
        is Right -> {
            call.respond(
                OK, responseV2(
                    id = resultat.right,
                    key = publicTopicKeyFunction(resultat.right)
                )
            )
        }

        is Left -> {
            logger.error("Kunne ikke opprette nøkkel for ident, resultatet ble 'null' noe som ikke skal kunne skje.")
            val (code, msg) = when (resultat.left.code) {
                FailureCode.PDL_NOT_FOUND -> HttpStatusCode.NotFound to "Person ikke i PDL"
                FailureCode.EXTERNAL_TECHINCAL_ERROR -> HttpStatusCode.ServiceUnavailable to "Ekstern feil, prøv igjen senere"
                FailureCode.INTERNAL_TECHINCAL_ERROR,
                FailureCode.DB_NOT_FOUND,
                FailureCode.CONFLICT -> InternalServerError to "Intern feil, rapporter til teamet med: kode=${resultat.left.code}, callId='${callId.value}'"
            }
            call.respondText(msg, status = code)
        }
    }
}
