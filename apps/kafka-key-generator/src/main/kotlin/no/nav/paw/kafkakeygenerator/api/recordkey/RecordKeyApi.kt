package no.nav.paw.kafkakeygenerator.api.recordkey

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.api.recordkey.functions.recordKey
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.utils.getCallId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Routing.configureRecordKeyApi(
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