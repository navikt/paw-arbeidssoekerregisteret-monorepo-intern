package no.nav.paw.bekreftelse.api.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.kafka.BekreftelseProducer
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.InnloggetBruker
import no.nav.paw.bekreftelse.api.model.InternState
import no.nav.paw.bekreftelse.api.model.Sluttbruker
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.model.toApi
import no.nav.paw.bekreftelse.api.model.toHendelse
import no.nav.paw.bekreftelse.api.model.toResponse
import no.nav.paw.bekreftelse.api.utils.logger
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyQueryMetadata
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore

class BekreftelseService(
    private val applicationConfig: ApplicationConfig,
    private val httpClient: HttpClient,
    private val kafkaStreams: KafkaStreams,
    private val bekreftelseProducer: BekreftelseProducer
) {
    private val mockDataService = MockDataService()
    private var internStateStore: ReadOnlyKeyValueStore<Long, InternState>? = null

    private fun getInternStateStore(): ReadOnlyKeyValueStore<Long, InternState> {
        if (!kafkaStreams.state().isRunningOrRebalancing) {
            throw IllegalStateException("Kafka Streams kjører ikke")
        }
        if (internStateStore == null) {
            internStateStore = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    applicationConfig.kafkaTopology.internStateStoreName,
                    QueryableStoreTypes.keyValueStore()
                )
            )
        }
        return checkNotNull(internStateStore) { "Intern state store er ikke initiert" }
    }

    @WithSpan
    suspend fun finnTilgjengeligBekreftelser(
        sluttbruker: Sluttbruker,
        innloggetBruker: InnloggetBruker,
        request: TilgjengeligeBekreftelserRequest,
        useMockData: Boolean
    ): TilgjengeligBekreftelserResponse {
        // TODO Fjern når vi har ferdig Kafka-logikk
        if (useMockData) {
            return mockDataService.finnTilgjengeligBekreftelser(sluttbruker.identitetsnummer)
        }

        val internState = getInternStateStore().get(sluttbruker.arbeidssoekerId)

        if (internState != null) {
            logger.info("Fant ${internState.tilgjendeligeBekreftelser.size} tilgjengelige bekreftelser")
            return internState.tilgjendeligeBekreftelser.toResponse()
        } else {
            return finnTilgjengeligBekreftelserFraAnnenNode(sluttbruker, innloggetBruker, request)
        }
    }

    @WithSpan
    suspend fun mottaBekreftelse(
        sluttbruker: Sluttbruker,
        innloggetBruker: InnloggetBruker,
        request: BekreftelseRequest,
        useMockData: Boolean
    ) {
        // TODO Fjern når vi har ferdig Kafka-logikk
        if (useMockData) {
            return mockDataService.mottaBekreftelse(sluttbruker.identitetsnummer, request.bekreftelseId)
        }

        val internState = getInternStateStore().get(sluttbruker.arbeidssoekerId)

        if (internState != null) {
            val tilgjengeligBekreftelse = internState.tilgjendeligeBekreftelser
                .firstOrNull { it.bekreftelseId == request.bekreftelseId }
            if (tilgjengeligBekreftelse != null) {
                logger.info("Rapportering med id ${request.bekreftelseId} funnet")
                val bekreftelse = request.toHendelse(
                    periodeId = tilgjengeligBekreftelse.periodeId,
                    gjelderFra = tilgjengeligBekreftelse.gjelderFra,
                    gjelderTil = tilgjengeligBekreftelse.gjelderTil,
                    innloggetBruker.ident,
                    innloggetBruker.type.toApi()
                )
                bekreftelseProducer.produceMessage(sluttbruker.kafkaKey, bekreftelse)
            } else {
                // TODO Rekreftelse ikke funnet. Hva gjør vi?
            }
        } else {
            sendBekreftelseTilAnnenNode(sluttbruker, innloggetBruker, request)
        }
    }

    private suspend fun finnTilgjengeligBekreftelserFraAnnenNode(
        sluttbruker: Sluttbruker,
        innloggetBruker: InnloggetBruker,
        request: TilgjengeligeBekreftelserRequest
    ): TilgjengeligBekreftelserResponse {
        val metadata = kafkaStreams.queryMetadataForKey(
            applicationConfig.kafkaTopology.internStateStoreName,
            sluttbruker.arbeidssoekerId,
            Serdes.Long().serializer()
        )

        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            logger.info("Fant ikke metadata for arbeidsoeker, $metadata")
            return emptyList()
        } else {
            val nodeUrl = "http://${metadata.activeHost().host()}/api/v1/tilgjengelige-rapporteringer"
            val response = httpClient.post(nodeUrl) {
                bearerAuth(innloggetBruker.bearerToken)
                setBody(request)
            }
            // TODO Error handling
            return response.body()
        }
    }

    private suspend fun sendBekreftelseTilAnnenNode(
        sluttbruker: Sluttbruker,
        innloggetBruker: InnloggetBruker,
        request: BekreftelseRequest
    ) {
        val metadata = kafkaStreams.queryMetadataForKey(
            applicationConfig.kafkaTopology.internStateStoreName,
            sluttbruker.arbeidssoekerId,
            Serdes.Long().serializer()
        )

        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            logger.info("Fant ikke metadata for arbeidsoeker, $metadata")
            // TODO Not found exception
        } else {
            val nodeUrl = "http://${metadata.activeHost().host()}/api/v1/rapportering"
            val response = httpClient.post(nodeUrl) {
                bearerAuth(innloggetBruker.bearerToken)
                setBody(request)
            }
            // TODO Error handling
        }
    }
}