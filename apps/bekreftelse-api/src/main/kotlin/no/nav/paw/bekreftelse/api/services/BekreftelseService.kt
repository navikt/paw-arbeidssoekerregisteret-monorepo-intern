package no.nav.paw.bekreftelse.api.services

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.consumer.BekreftelseHttpConsumer
import no.nav.paw.bekreftelse.api.context.SecurityContext
import no.nav.paw.bekreftelse.api.exception.DataIkkeFunnetException
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.InternState
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.model.asApi
import no.nav.paw.bekreftelse.api.model.toResponse
import no.nav.paw.bekreftelse.api.producer.BekreftelseKafkaProducer
import no.nav.paw.bekreftelse.api.utils.buildLogger
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyQueryMetadata
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore

class BekreftelseService(
    private val applicationConfig: ApplicationConfig,
    private val bekreftelseHttpConsumer: BekreftelseHttpConsumer,
    private val kafkaStreams: KafkaStreams,
    private val bekreftelseKafkaProducer: BekreftelseKafkaProducer
) {
    private val logger = buildLogger
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

    context(SecurityContext)
    @WithSpan
    suspend fun finnTilgjengeligBekreftelser(
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
            return finnTilgjengeligBekreftelserFraAnnenNode(request)
        }
    }

    context(SecurityContext)
    @WithSpan
    suspend fun mottaBekreftelse(
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
                val bekreftelse = request.asApi(
                    periodeId = tilgjengeligBekreftelse.periodeId,
                    gjelderFra = tilgjengeligBekreftelse.gjelderFra,
                    gjelderTil = tilgjengeligBekreftelse.gjelderTil,
                    innloggetBruker
                )
                bekreftelseKafkaProducer.produceMessage(sluttbruker.kafkaKey, bekreftelse)
            } else {
                // TODO Rekreftelse ikke funnet. Hva gjør vi?
            }
        } else {
            sendBekreftelseTilAnnenNode(request)
        }
    }

    context(SecurityContext)
    private suspend fun finnTilgjengeligBekreftelserFraAnnenNode(
        request: TilgjengeligeBekreftelserRequest
    ): TilgjengeligBekreftelserResponse {
        val metadata = kafkaStreams.queryMetadataForKey(
            applicationConfig.kafkaTopology.internStateStoreName,
            sluttbruker.arbeidssoekerId,
            Serdes.Long().serializer()
        )

        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            logger.warn("Fant ikke metadata for arbeidsoeker, $metadata")
            throw DataIkkeFunnetException("Fant ikke data for arbeidsoeker")
        } else {
            return bekreftelseHttpConsumer.finnTilgjengeligBekreftelser(
                host = metadata.activeHost().host(),
                bearerToken = accessToken.jwt,
                request = request
            )
        }
    }

    context(SecurityContext)
    private suspend fun sendBekreftelseTilAnnenNode(
        request: BekreftelseRequest
    ) {
        val metadata = kafkaStreams.queryMetadataForKey(
            applicationConfig.kafkaTopology.internStateStoreName,
            sluttbruker.arbeidssoekerId,
            Serdes.Long().serializer()
        )

        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            logger.warn("Fant ikke metadata for arbeidsoeker, $metadata")
            throw DataIkkeFunnetException("Fant ikke data for arbeidsoeker")
        } else {
            bekreftelseHttpConsumer.sendBekreftelse(
                host = metadata.activeHost().host(),
                bearerToken = accessToken.jwt,
                request = request
            )
        }
    }
}