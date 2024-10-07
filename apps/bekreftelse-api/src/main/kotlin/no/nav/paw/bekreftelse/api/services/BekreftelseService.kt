package no.nav.paw.bekreftelse.api.services

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.consumer.BekreftelseHttpConsumer
import no.nav.paw.bekreftelse.api.context.SecurityContext
import no.nav.paw.bekreftelse.api.exception.DataIkkeFunnetForIdException
import no.nav.paw.bekreftelse.api.exception.DataTilhoererIkkeBrukerException
import no.nav.paw.bekreftelse.api.exception.SystemfeilException
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
import org.apache.kafka.streams.state.HostInfo
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore

class BekreftelseService(
    private val serverConfig: ServerConfig,
    private val applicationConfig: ApplicationConfig,
    private val bekreftelseHttpConsumer: BekreftelseHttpConsumer,
    private val kafkaStreams: KafkaStreams,
    private val bekreftelseKafkaProducer: BekreftelseKafkaProducer
) {
    private val logger = buildLogger
    private val mockDataService = MockDataService()
    private var internStateStore: ReadOnlyKeyValueStore<Long, InternState>? = null

    @WithSpan
    suspend fun finnTilgjengeligBekreftelser(
        securityContext: SecurityContext,
        request: TilgjengeligeBekreftelserRequest,
        useMockData: Boolean
    ): TilgjengeligBekreftelserResponse {
        // TODO Fjern når vi har ferdig Kafka-logikk
        if (useMockData) {
            return mockDataService.finnTilgjengeligBekreftelser(securityContext.sluttbruker.identitetsnummer)
        }

        logger.info("Skal hente tilgjengelige bekreftelser")

        val internState = getInternStateStore().get(securityContext.sluttbruker.arbeidssoekerId)

        if (internState != null) {
            logger.info("Fant {} tilgjengelige bekreftelser i lokal state", internState.tilgjendeligeBekreftelser.size)
            return internState.tilgjendeligeBekreftelser.toResponse()
        } else {
            return finnTilgjengeligBekreftelserFraAnnenNode(securityContext, request)
        }
    }

    @WithSpan
    suspend fun mottaBekreftelse(
        securityContext: SecurityContext,
        request: BekreftelseRequest,
        useMockData: Boolean
    ) {
        // TODO Fjern når vi har ferdig Kafka-logikk
        if (useMockData) {
            return mockDataService.mottaBekreftelse(securityContext.sluttbruker.identitetsnummer, request.bekreftelseId)
        }

        val internState = getInternStateStore().get(securityContext.sluttbruker.arbeidssoekerId)

        logger.info("Har mottatt bekreftelse")

        if (internState != null) {
            val tilgjengeligBekreftelse = internState.tilgjendeligeBekreftelser
                .firstOrNull { it.bekreftelseId == request.bekreftelseId }
            if (tilgjengeligBekreftelse != null) {
                logger.info("Mottok svar for bekreftelse som er i lokal state")
                if (tilgjengeligBekreftelse.arbeidssoekerId != securityContext.sluttbruker.arbeidssoekerId) {
                    throw DataTilhoererIkkeBrukerException("Bekreftelse tilhører ikke bruker")
                }
                val bekreftelse = request.asApi(
                    periodeId = tilgjengeligBekreftelse.periodeId,
                    gjelderFra = tilgjengeligBekreftelse.gjelderFra,
                    gjelderTil = tilgjengeligBekreftelse.gjelderTil,
                    securityContext.innloggetBruker
                )
                bekreftelseKafkaProducer.produceMessage(securityContext.sluttbruker.kafkaKey, bekreftelse)
            } else {
                throw DataIkkeFunnetForIdException("Fant ingen bekreftelse for gitt id")
            }
        } else {
            sendBekreftelseTilAnnenNode(securityContext, request)
        }
    }

    private suspend fun finnTilgjengeligBekreftelserFraAnnenNode(
        securityContext: SecurityContext,
        request: TilgjengeligeBekreftelserRequest
    ): TilgjengeligBekreftelserResponse {
        val hostInfo = kafkaStreams.hentHostInfoFraKafka(securityContext.sluttbruker.arbeidssoekerId)
        val hostInfoForKey = kafkaStreams.hentHostInfoFraKafka(securityContext.sluttbruker.kafkaKey)
        logger.debug("Med store key: {} med stream key {}", hostInfo, hostInfoForKey)
        val host = "${hostInfo.host()}:${hostInfo.port()}"
        logger.debug("Sjekker om informasjon finnes på node {}", host)

        if (hostInfo.host() == serverConfig.ip) {
            logger.info("Fant ingen tilgjengelige bekreftelser for arbeidssøker")
            return emptyList()
        }

        logger.info("Må hente tilgjengelige bekreftelser fra node {}", host)
        val tilgjendeligeBekreftelser = bekreftelseHttpConsumer.finnTilgjengeligBekreftelser(
            host = host,
            bearerToken = securityContext.accessToken.jwt,
            request = request
        )
        logger.info("Fant {} tilgjengelige bekreftelser på node {}", tilgjendeligeBekreftelser.size, host)
        return tilgjendeligeBekreftelser
    }

    private suspend fun sendBekreftelseTilAnnenNode(
        securityContext: SecurityContext,
        request: BekreftelseRequest
    ) {
        val hostInfo = kafkaStreams.hentHostInfoFraKafka(securityContext.sluttbruker.arbeidssoekerId)
        val host = "${hostInfo.host()}:${hostInfo.port()}"
        logger.debug("Sjekker om informasjon finnes på node {}", host)

        if (hostInfo.host() == serverConfig.ip) {
            throw DataIkkeFunnetForIdException("Fant ingen bekreftelse for gitt id")
        }

        logger.info("Oversender svar for bekreftelse som er på node {}", host)
        bekreftelseHttpConsumer.sendBekreftelse(
            host = "${hostInfo.host()}:${hostInfo.port()}",
            bearerToken = securityContext.accessToken.jwt,
            request = request
        )
    }

    private fun KafkaStreams.hentHostInfoFraKafka(key: Long): HostInfo {
        val metadata = queryMetadataForKey(
            applicationConfig.kafkaTopology.internStateStoreName,
            key,
            Serdes.Long().serializer()
        )
        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            logger.error("Fant ikke metadata for arbeidsoeker, {}", metadata)
            throw SystemfeilException("Fant ikke metadata for arbeidsøker i Kafka Streams")
        } else {
            return metadata.activeHost()
        }
    }

    private fun getInternStateStore(): ReadOnlyKeyValueStore<Long, InternState> {
        if (!kafkaStreams.state().isRunningOrRebalancing) {
            throw SystemfeilException("Kafka Streams kjører ikke")
        }
        if (internStateStore == null) {
            internStateStore = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    applicationConfig.kafkaTopology.internStateStoreName,
                    QueryableStoreTypes.keyValueStore()
                )
            )
        }
        return internStateStore ?: throw SystemfeilException("Intern state store er ikke initiert")
    }
}