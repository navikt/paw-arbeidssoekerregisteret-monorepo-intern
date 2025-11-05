package no.nav.paw.kafkakeygenerator.api.v2

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.api.models.IdentitetRequest
import no.nav.paw.kafkakeygenerator.api.models.IdentitetResponse
import no.nav.paw.kafkakeygenerator.service.IdentitetResponseService
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.utils.asRecordKey
import no.nav.paw.kafkakeygenerator.utils.getCallId
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering

fun Routing.apiV2Routes(
    kafkaKeysService: KafkaKeysService,
    identitetResponseService: IdentitetResponseService
) {
    route("/api/v2") {
        autentisering(AzureAd) {
            post("/hent") {
                val callId = call.request.getCallId
                val request = call.receive<RequestV2>()
                val identitetsnummer = Identitetsnummer(request.ident)
                val arbeidssoekerId = kafkaKeysService.hentEllerOppdater(callId, identitetsnummer)
                val response = ResponseV2(
                    id = arbeidssoekerId.value,
                    key = arbeidssoekerId.value.asRecordKey()
                )
                call.respond<ResponseV2>(response)
            }

            post("/hentEllerOpprett") {
                val callId = call.request.getCallId
                val request = call.receive<RequestV2>()
                val arbeidssoekerId = kafkaKeysService.hentEllerOpprett(callId, Identitetsnummer(request.ident))
                val response = ResponseV2(
                    id = arbeidssoekerId.value,
                    key = arbeidssoekerId.value.asRecordKey()
                )
                call.respond<ResponseV2>(response)
            }

            post("/info") {
                val callId = call.request.getCallId
                val request = call.receive<RequestV2>()
                val response = kafkaKeysService.hentInfo(callId, Identitetsnummer(request.ident))
                call.respond<InfoResponse>(response)
            }

            post("/lokalInfo") {
                val request = call.receive<AliasRequest>()
                val response = kafkaKeysService.hentLokaleAlias(request.antallPartisjoner, request.identer)
                call.respond<AliasResponse>(response)
            }

            post("identiteter") {
                val request = call.receive<IdentitetRequest>()
                val visKonflikter = call.queryParameters["visKonflikter"]?.toBooleanStrictOrNull() ?: false
                val hentPdl = call.queryParameters["hentPdl"]?.toBooleanStrictOrNull() ?: false
                val response = identitetResponseService.finnForIdentitet(
                    identitet = request.identitet,
                    visKonflikter = visKonflikter,
                    hentPdl = hentPdl
                )
                call.respond<IdentitetResponse>(response)
            }
        }
    }
}
