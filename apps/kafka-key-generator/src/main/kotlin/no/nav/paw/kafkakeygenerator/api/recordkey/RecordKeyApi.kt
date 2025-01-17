package no.nav.paw.kafkakeygenerator.api.recordkey

import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.api.recordkey.functions.recordKey
import no.nav.paw.kafkakeygenerator.config.AuthenticationConfig
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

fun Routing.configureRecordKeyApi(
    authenticationConfig: AuthenticationConfig,
    kafkaKeysService: KafkaKeysService
) {
    val logger = LoggerFactory.getLogger("record-key-api")
    authenticate(authenticationConfig.kafkaKeyApiAuthProvider) {
        post("/api/v1/record-key") {
            handleRequest(kafkaKeysService, logger)
        }
    }
}

@WithSpan
private suspend fun RoutingContext.handleRequest(
    kafkaKeysService: KafkaKeysService,
    logger: Logger
) {
    val callId = call.request.headers["traceparent"]
        ?.let(::CallId)
        ?: CallId(UUID.randomUUID().toString())
    val identitetsnummer = Identitetsnummer(call.receive<RecordKeyLookupRequestV1>().ident)
    val (status, response) = kafkaKeysService::hent.recordKey(logger, callId, identitetsnummer)
    call.respond(status, response)
}