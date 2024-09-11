package no.nav.paw.bekreftelse.api

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.kafka.BekreftelseProducer
import no.nav.paw.bekreftelse.api.kafka.InternState
import no.nav.paw.bekreftelse.api.kafka.InternStateSerde
import no.nav.paw.bekreftelse.api.kafka.appTopology
import no.nav.paw.bekreftelse.api.services.AutorisasjonService
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory

fun createDependencies(
    applicationConfig: ApplicationConfig,
    kafkaConfig: KafkaConfig,
    kafkaStreamsConfig: KafkaConfig,
    azureM2MConfig: AzureM2MConfig,
    kafkaKeyConfig: KafkaKeyConfig
): Dependencies {
    val logger = LoggerFactory.getLogger("rapportering-api")

    val azureM2MTokenClient = azureAdM2MTokenClient(applicationConfig.naisEnv, azureM2MConfig)

    val kafkaKeysClient = kafkaKeysClient(kafkaKeyConfig) {
        azureM2MTokenClient.createMachineToMachineToken(kafkaKeyConfig.scope)
    }

    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
    }

    val streamsConfig = KafkaStreamsFactory(applicationConfig.applicationIdSuffix, kafkaStreamsConfig)
        .withDefaultKeySerde(Serdes.LongSerde::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)

    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(applicationConfig.bekreftelseStateStoreName),
                Serdes.Long(),
                InternStateSerde(),
            )
        )

    val topology = streamsBuilder.appTopology(
        prometheusRegistry = prometheusMeterRegistry,
        bekreftelseHendelseLoggTopic = applicationConfig.bekreftelseHendelseLoggTopic,
        bekreftelseStateStoreName = applicationConfig.bekreftelseStateStoreName,
    )

    val kafkaStreams = KafkaStreams(
        topology,
        streamsConfig.properties.apply {
            put("application.server", applicationConfig.hostname)
        }
    )

    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil: ${throwable.message}", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }

    kafkaStreams.start()

    val bekreftelseStateStore: ReadOnlyKeyValueStore<Long, InternState> = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            applicationConfig.bekreftelseStateStoreName,
            QueryableStoreTypes.keyValueStore()
        )
    )

    val health = Health(kafkaStreams)

    val bekreftelseProducer = BekreftelseProducer(kafkaConfig, applicationConfig)


    val poaoTilgangClient = PoaoTilgangCachedClient(
        PoaoTilgangHttpClient(
            applicationConfig.poaoClientConfig.url,
            { azureM2MTokenClient.createMachineToMachineToken(applicationConfig.poaoClientConfig.scope) }
        )
    )

    val autorisasjonService = AutorisasjonService(poaoTilgangClient)

    return Dependencies(
        kafkaKeysClient,
        httpClient,
        kafkaStreams,
        prometheusMeterRegistry,
        bekreftelseStateStore,
        health,
        bekreftelseProducer,
        autorisasjonService
    )
}

data class Dependencies(
    val kafkaKeysClient: KafkaKeysClient,
    val httpClient: HttpClient,
    val kafkaStreams: KafkaStreams,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val bekreftelseStateStore: ReadOnlyKeyValueStore<Long, InternState>,
    val health: Health,
    val bekreftelseProducer: BekreftelseProducer,
    val autorisasjonService: AutorisasjonService
)