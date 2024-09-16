package no.nav.paw.bekreftelse.api.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.kafka.BekreftelseProducer
import no.nav.paw.bekreftelse.api.kafka.createMelding
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.InternState
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelse
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.model.toResponse
import no.nav.paw.bekreftelse.api.utils.logger
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyQueryMetadata
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import java.time.Duration
import java.time.Instant
import java.util.*

interface BekreftelseService {

    suspend fun finnTilgjengeligBekreftelser(
        bearerToken: String,
        arbeidssoekerId: Long,
        request: TilgjengeligeBekreftelserRequest
    ): TilgjengeligBekreftelserResponse

    suspend fun mottaBekreftelse(
        bearerToken: String,
        arbeidssoekerId: Long,
        request: BekreftelseRequest
    )
}

// TODO Slett når vi har ferdig Kafka-logikk
class BekreftelseServiceMock : BekreftelseService {

    private var tilgjengeligBekreftelser = mutableMapOf<String, TilgjengeligBekreftelserResponse>()
    private val fnr1 = "17830348441"
    private val fnr2 = "19519238708"
    private val fnr3 = "02837797848"
    private val fnr4 = "16868598968"
    private val fnr5 = "28878098821"
    private val periodeId1 = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1feb")
    private val periodeId2 = UUID.fromString("ec6b5a10-b67c-42c1-b6e7-a642c36bd78e")
    private val periodeId3 = UUID.fromString("44a4375c-b7ab-40ea-83f5-0eb9869925eb")
    private val periodeId4: UUID = UUID.fromString("bbf3e9eb-6d7b-465b-bf79-ae6c82cf1ddd")
    private val periodeId5: UUID = UUID.fromString("6ea57aec-353c-4df5-935f-9bead8afb221")
    private val bekreftelseId1: UUID = UUID.fromString("f45ffbf3-e4d5-49fd-b5b7-17aaee478dfc")
    private val bekreftelseId2: UUID = UUID.fromString("0cae8890-5500-4f5f-8fc1-9a0aae3b35a0")
    private val bekreftelseId3: UUID = UUID.fromString("4f5e7f5c-1fe3-4b27-a07b-34ff9f4ea23f")
    private val bekreftelseId4a: UUID = UUID.fromString("47e5c02d-abab-4e75-951c-db6c985901e4")
    private val bekreftelseId4b: UUID = UUID.fromString("77322685-80db-41db-b79f-86915a9a5d9a")
    private val bekreftelseId5a: UUID = UUID.fromString("992d5363-bab4-4b1d-987e-3e8eb4db3f64")
    private val bekreftelseId5b: UUID = UUID.fromString("9777408c-938d-41e6-b9fd-5177120695d6")

    init {
        tilgjengeligBekreftelser[fnr1] = listOf(
            TilgjengeligBekreftelse(periodeId1, bekreftelseId1, pastInstant(), futureInstant())
        )
        tilgjengeligBekreftelser[fnr2] = listOf(
            TilgjengeligBekreftelse(periodeId2, bekreftelseId2, pastInstant(), futureInstant())
        )
        tilgjengeligBekreftelser[fnr3] = listOf(
            TilgjengeligBekreftelse(periodeId3, bekreftelseId3, pastInstant(), futureInstant())
        )
        val fra4a = pastInstant()
        val fra4b = fra4a.minus(pastDuration())
        tilgjengeligBekreftelser[fnr4] = listOf(
            TilgjengeligBekreftelse(periodeId4, bekreftelseId4a, fra4a, futureInstant()),
            TilgjengeligBekreftelse(periodeId4, bekreftelseId4b, pastInstant(), fra4b)
        )
        val fra5a = pastInstant()
        val fra5b = fra5a.minus(pastDuration())
        tilgjengeligBekreftelser[fnr5] = listOf(
            TilgjengeligBekreftelse(periodeId5, bekreftelseId5a, fra5a, futureInstant()),
            TilgjengeligBekreftelse(periodeId5, bekreftelseId5b, fra5b, fra5a)
        )
    }

    private fun pastInstant(): Instant {
        return Instant.now().minus(pastDuration())
    }

    private fun futureInstant(): Instant {
        return Instant.now().minus(futureDuration())
    }

    private fun pastDuration(): Duration {
        val days = Random().nextLong(10, 90)
        return Duration.ofDays(days)
    }

    private fun futureDuration(): Duration {
        val days = Random().nextLong(0, 90)
        return Duration.ofDays(days)
    }

    override suspend fun finnTilgjengeligBekreftelser(
        bearerToken: String,
        arbeidssoekerId: Long,
        request: TilgjengeligeBekreftelserRequest
    ): TilgjengeligBekreftelserResponse {
        val key = request.identitetsnummer ?: arbeidssoekerId.toString()
        return tilgjengeligBekreftelser[key] ?: emptyList()
    }

    override suspend fun mottaBekreftelse(
        bearerToken: String,
        arbeidssoekerId: Long,
        request: BekreftelseRequest
    ) {
        val key = request.identitetsnummer ?: arbeidssoekerId.toString()
        val eksisterende = tilgjengeligBekreftelser[key]
        if (eksisterende != null) {
            val oppdatert = eksisterende.filter { it.bekreftelseId != request.bekreftelseId }
            tilgjengeligBekreftelser[key] = oppdatert
        }
    }
}

class BekreftelseServiceImpl(
    private val applicationConfig: ApplicationConfig,
    private val httpClient: HttpClient,
    private val kafkaStreams: KafkaStreams,
    private val bekreftelseProducer: BekreftelseProducer,
) : BekreftelseService {
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

    override suspend fun finnTilgjengeligBekreftelser(
        bearerToken: String,
        arbeidssoekerId: Long,
        request: TilgjengeligeBekreftelserRequest
    ): TilgjengeligBekreftelserResponse {
        val internState = getInternStateStore().get(arbeidssoekerId)

        if (internState != null) {
            logger.info("Fant ${internState.tilgjendeligeBekreftelser.size} tilgjengelige bekreftelser")
            return internState.tilgjendeligeBekreftelser.toResponse()
        } else {
            return finnTilgjengeligBekreftelserFraAnnenNode(bearerToken, arbeidssoekerId, request)
        }
    }

    override suspend fun mottaBekreftelse(
        bearerToken: String,
        arbeidssoekerId: Long,
        request: BekreftelseRequest
    ) {
        val internState = getInternStateStore().get(arbeidssoekerId)

        if (internState != null) {
            internState.tilgjendeligeBekreftelser
                .firstOrNull { it.bekreftelseId == request.bekreftelseId }
                ?.let {
                    logger.info("Rapportering med id ${request.bekreftelseId} funnet")
                    val rapporteringsMelding = createMelding(it, request)
                    bekreftelseProducer.produceMessage(arbeidssoekerId, rapporteringsMelding)
                }
        } else {
            sendBekreftelseTilAnnenNode(bearerToken, arbeidssoekerId, request)
        }
    }

    private suspend fun finnTilgjengeligBekreftelserFraAnnenNode(
        bearerToken: String,
        arbeidssoekerId: Long,
        request: TilgjengeligeBekreftelserRequest
    ): TilgjengeligBekreftelserResponse {
        val metadata = kafkaStreams.queryMetadataForKey(
            applicationConfig.kafkaTopology.internStateStoreName, arbeidssoekerId, Serdes.Long().serializer()
        )

        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            logger.info("Fant ikke metadata for arbeidsoeker, $metadata")
            return emptyList()
        } else {
            val response = httpClient.post(
                "http://${
                    metadata.activeHost().host()
                }/api/v1/tilgjengelige-rapporteringer"
            ) {
                bearerAuth(bearerToken)
                setBody(request)
            }
            // TODO Error handling
            return response.body()
        }
    }

    private suspend fun sendBekreftelseTilAnnenNode(
        bearerToken: String,
        arbeidssoekerId: Long,
        request: BekreftelseRequest
    ) {
        val metadata = kafkaStreams.queryMetadataForKey(
            applicationConfig.kafkaTopology.internStateStoreName, arbeidssoekerId, Serdes.Long().serializer()
        )

        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            logger.info("Fant ikke metadata for arbeidsoeker, $metadata")
            // TODO Not found exception
        } else {
            val response = httpClient.post("http://${metadata.activeHost().host()}/api/v1/rapportering") {
                bearerAuth(bearerToken)
                setBody(request)
            }
            // TODO Error handling
        }
    }
}