package no.nav.paw.kafkakeygenerator.api.v2

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.utils.getCallId
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.Left
import no.nav.paw.kafkakeygenerator.vo.Right
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Routing.konfigurerApiV2(
    kafkaKeysService: KafkaKeysService
) {
    val logger = LoggerFactory.getLogger("api")
    route("/api/v2") {
        autentisering(AzureAd) {
            post("/hent") {
                hent(kafkaKeysService, logger)
            }
            post("/hentEllerOpprett") {
                hentEllerOpprett(kafkaKeysService, logger)
            }
            post("/info") {
                hentInfo(kafkaKeysService, logger)
            }
            post("/lokalInfo") {
                hentLokalInfo(kafkaKeysService, logger)
            }
        }
    }
}

@WithSpan
suspend fun RoutingContext.hentLokalInfo(
    kafkaKeysService: KafkaKeysService,
    logger: Logger
) {
    val request = call.receive<AliasRequest>()
    when (val resultat = kafkaKeysService.hentLokaleAlias(request.antallPartisjoner, request.identer)) {
        is Right -> call.respond(
            OK, AliasResponse(
                alias = resultat.right
            )
        )

        is Left -> {
            logger.error("Kunne ikke hente alias for identer: {}", resultat.left.code, resultat.left.exception)
            call.respond(
                status = InternalServerError,
                message = resultat.left.code.name
            )
        }
    }
}

@WithSpan
suspend fun RoutingContext.hentInfo(
    kafkaKeysService: KafkaKeysService,
    logger: Logger
) {
    val callId = call.request.getCallId
    val request = call.receive<RequestV2>()
    when (val resultat = kafkaKeysService.validerLagretData(callId, Identitetsnummer(request.ident))) {
        is Right -> call.respond(resultat.right)

        is Left -> {
            logger.error("Kunne ikke hente info for ident: {}", resultat.left.code, resultat.left.exception)
            call.respond(
                status = InternalServerError,
                message = resultat.left.code.name
            )
        }
    }
}

@WithSpan
private suspend fun RoutingContext.hent(
    kafkaKeysService: KafkaKeysService,
    logger: Logger
) {
    val callId = call.request.getCallId
    val request = call.receive<RequestV2>()
    when (val resultat = kafkaKeysService.kunHent(Identitetsnummer(request.ident))) {
        is Right -> {
            call.respond(
                OK, responseV2(
                    id = resultat.right,
                    key = publicTopicKeyFunction(resultat.right)
                )
            )
        }

        is Left -> {
            logger.error("Henting av nøkkel feilet")
            val (code, msg) = when (resultat.left.code) {
                FailureCode.DB_NOT_FOUND -> HttpStatusCode.NotFound to "Nøkkel ikke funnet for ident"
                FailureCode.PDL_NOT_FOUND -> HttpStatusCode.NotFound to "Person ikke funnet i PDL"
                FailureCode.EXTERNAL_TECHINCAL_ERROR -> HttpStatusCode.ServiceUnavailable to "Ekstern feil, prøv igjen senere"
                FailureCode.INTERNAL_TECHINCAL_ERROR,
                FailureCode.CONFLICT -> InternalServerError to "Intern feil, rapporter til teamet med: kode=${resultat.left.code}, callId='${callId.value}'"
            }
            call.respondText(msg, status = code)
        }
    }
}

@WithSpan
private suspend fun RoutingContext.hentEllerOpprett(
    kafkaKeysService: KafkaKeysService,
    logger: Logger
) {
    val callId = call.request.getCallId
    val request = call.receive<RequestV2>()
    when (val resultat = kafkaKeysService.hentEllerOpprett(callId, Identitetsnummer(request.ident))) {
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
