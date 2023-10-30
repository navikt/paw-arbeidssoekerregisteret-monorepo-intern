package no.nav.paw.kafkakeygenerator.api.v1

import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.paw.kafkakeygenerator.Applikasjon
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
    authenticate(autentiseringKonfigurasjon.name) {
        post("/api/v1/hentEllerOpprett") {
            val callId = call.request.headers["traceparent"]
                ?.let { CallId(it) }
                ?: CallId(UUID.randomUUID().toString())
            val request = call.receive<Request>()
            val nøkkel = applikasjon.hentEllerOpprett(callId, Identitetsnummer(request.ident))
            if (nøkkel != null) {
                call.respond(OK, Response(nøkkel))
            } else {
                logger.error("Kunne ikke opprette nøkkel for ident, resultatet ble 'null' noe som ikke skal kunne skje.")
                call.respondText("Intern feil, prøv igjen senere", status = InternalServerError)
            }
        }
    }
}
