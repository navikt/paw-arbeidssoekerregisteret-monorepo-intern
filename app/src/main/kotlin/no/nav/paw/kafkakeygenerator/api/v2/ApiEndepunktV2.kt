package no.nav.paw.kafkakeygenerator.api.v2

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

fun Routing.konfigurerApiV2(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    applikasjon: Applikasjon
) {
    val logger = LoggerFactory.getLogger("api")
    authenticate(autentiseringKonfigurasjon.kafkaKeyApiAuthProvider) {
        post("/api/v2/hentEllerOpprett") {
            val callId = call.request.headers["traceparent"]
                ?.let { CallId(it) }
                ?: CallId(UUID.randomUUID().toString())
            val request = call.receive<RequestV2>()
            val nøkkel = applikasjon.hentEllerOpprett(callId, Identitetsnummer(request.ident))
            if (nøkkel != null) {
                call.respond(
                    status = OK,
                    message = ResponseV2(
                        id = nøkkel,
                        key = publicTopicKeyFunction(nøkkel)
                    )
                )
            } else {
                logger.error("Kunne ikke opprette id for ident, resultatet ble 'null' noe som ikke skal kunne skje.")
                call.respondText("Intern feil, prøv igjen senere", status = InternalServerError)
            }
        }
    }
}
