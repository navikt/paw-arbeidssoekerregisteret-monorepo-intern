package no.nav.paw.kafkakeygenerator.api.v1

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.FailureCode
import no.nav.paw.kafkakeygenerator.FailureCode.*
import no.nav.paw.kafkakeygenerator.Left
import no.nav.paw.kafkakeygenerator.Right
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.slf4j.LoggerFactory
import java.util.*

fun Routing.konfigurerApi(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    applikasjon: Applikasjon
) {
    val logger = LoggerFactory.getLogger("api")
    authenticate(autentiseringKonfigurasjon.kafkaKeyApiAuthProvider) {
        post("/api/v1/hentEllerOpprett") {
            val callId = call.request.headers["traceparent"]
                ?.let { CallId(it) }
                ?: CallId(UUID.randomUUID().toString())
            val request = call.receive<Request>()
            val resultat = applikasjon.hentEllerOpprett(callId, Identitetsnummer(request.ident))
                .map(::Response)

            when (resultat) {
                is Right -> {
                    call.respond(OK, resultat.right)
                }
                is Left -> {
                    logger.error("Kunne ikke opprette nøkkel for ident, resultatet ble 'null' noe som ikke skal kunne skje.")
                    val (code, msg) = when (resultat.left.code) {
                        PDL_NOT_FOUND -> NotFound to "Person ikke i PDL"
                        EXTERNAL_TECHINCAL_ERROR -> ServiceUnavailable to "Ekstern feil, prøv igjen senere"
                        INTERNAL_TECHINCAL_ERROR,
                        DB_NOT_FOUND,
                        CONFLICT -> InternalServerError to "Intern feil, rapporter til teamet med: kode=${resultat.left.code}, callId='${callId.value}'"
                    }
                    call.respondText(msg, status = code)
                }
            }
        }
    }
}
