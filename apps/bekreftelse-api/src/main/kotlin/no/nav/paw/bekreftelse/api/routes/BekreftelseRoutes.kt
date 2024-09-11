package no.nav.paw.bekreftelse.api.routes

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.bekreftelse.api.domain.BekreftelseRequest
import no.nav.paw.bekreftelse.api.domain.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.domain.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.domain.toResponse
import no.nav.paw.bekreftelse.api.kafka.BekreftelseProducer
import no.nav.paw.bekreftelse.api.kafka.InternState
import no.nav.paw.bekreftelse.api.kafka.createMelding
import no.nav.paw.bekreftelse.api.services.AutorisasjonService
import no.nav.paw.bekreftelse.api.utils.logger
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.poao_tilgang.client.TilgangType
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyQueryMetadata
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore

fun Route.bekreftelseRoutes(
    kafkaKeyClient: KafkaKeysClient,
    stateStoreName: String,
    bekreftelseStateStore: ReadOnlyKeyValueStore<Long, InternState>,
    kafkaStreams: KafkaStreams,
    httpClient: HttpClient,
    bekreftelseProducer: BekreftelseProducer,
    autorisasjonService: AutorisasjonService
) {
    route("/api/v1") {
        authenticate("tokenx", "azure") {
            post<TilgjengeligeBekreftelserRequest>("/tilgjengelige-rapporteringer") { request ->
                with(requestScope(request.identitetsnummer, autorisasjonService, kafkaKeyClient, TilgangType.LESE)) {
                    val arbeidssoekerId = this

                    bekreftelseStateStore
                        .get(arbeidssoekerId)
                        ?.tilgjendeligeBekreftelser
                        ?.toResponse()
                        ?.let { rapporteringerTilgjengelig ->
                            logger.info("Fant ${rapporteringerTilgjengelig.size} rapporteringer")
                            return@post call.respond(HttpStatusCode.OK, rapporteringerTilgjengelig)
                        }

                    val metadata = kafkaStreams.queryMetadataForKey(
                        stateStoreName, arbeidssoekerId, Serdes.Long().serializer()
                    )

                    if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
                        logger.info("Fant ikke metadata for arbeidsoeker, $metadata")
                        return@post call.respond(HttpStatusCode.OK, emptyList<TilgjengeligBekreftelserResponse>())
                    } else {
                        val response = httpClient.post(
                            "http://${
                                metadata.activeHost().host()
                            }/api/v1/tilgjengelige-rapporteringer"
                        ) {
                            call.request.headers["Authorization"]?.let { bearerAuth(it) }
                            setBody(request)
                        }
                        return@post call.respond(response.status, response.body())
                    }
                }

            }
            post<BekreftelseRequest>("/rapportering") { rapportering ->
                with(
                    requestScope(
                        rapportering.identitetsnummer,
                        autorisasjonService,
                        kafkaKeyClient,
                        TilgangType.SKRIVE
                    )
                ) {
                    val arbeidsoekerId = this

                    bekreftelseStateStore
                        .get(arbeidsoekerId)
                        ?.tilgjendeligeBekreftelser
                        ?.firstOrNull { it.bekreftelseId == rapportering.bekreftelseId }
                        ?.let {
                            logger.info("Rapportering med id ${rapportering.bekreftelseId} funnet")
                            val rapporteringsMelding = createMelding(it, rapportering)
                            bekreftelseProducer.produceMessage(arbeidsoekerId, rapporteringsMelding)

                            return@post call.respond(HttpStatusCode.OK)
                        }

                    val metadata = kafkaStreams.queryMetadataForKey(
                        stateStoreName, arbeidsoekerId, Serdes.Long().serializer()
                    )

                    if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
                        logger.info("Fant ikke metadata for arbeidsoeker, $metadata")
                        return@post call.respond(HttpStatusCode.NotFound)
                    } else {
                        val response = httpClient.post("http://${metadata.activeHost().host()}/api/v1/rapportering") {
                            call.request.headers["Authorization"]?.let { bearerAuth(it) }
                            setBody(rapportering)
                        }
                        return@post call.respond(response.status)
                    }
                }
            }
        }
    }
}

