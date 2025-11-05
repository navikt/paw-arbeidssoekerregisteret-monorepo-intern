package no.nav.paw.kafkakeygenerator.api.v1

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.utils.asRecordKey
import no.nav.paw.kafkakeygenerator.utils.getCallId
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering

fun Routing.apiV1Routes(
    kafkaKeysService: KafkaKeysService
) {
    route("/api/v1") {
        autentisering(AzureAd) {
            post("/record-key") {
                val callId = call.request.getCallId
                val request = call.receive<RequestV1>()
                val identitetsnummer = Identitetsnummer(request.ident)
                val arbeidssoekerId = kafkaKeysService.hentEllerOppdater(callId, identitetsnummer)
                val recordKey = arbeidssoekerId.value.asRecordKey()
                call.respond(ResponseV1(recordKey))
            }
        }
    }
}