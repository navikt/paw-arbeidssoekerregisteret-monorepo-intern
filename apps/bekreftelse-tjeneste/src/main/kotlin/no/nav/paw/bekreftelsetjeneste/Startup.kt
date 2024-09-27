package no.nav.paw.bekreftelsetjeneste

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import no.nav.paw.bekreftelsetjeneste.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.bekreftelsetjeneste.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.bekreftelsetjeneste.config.ServerConfig
import no.nav.paw.bekreftelsetjeneste.context.ApplicationContext
import no.nav.paw.bekreftelsetjeneste.plugins.buildKafkaStreams
import no.nav.paw.bekreftelsetjeneste.plugins.configureKafka
import no.nav.paw.bekreftelsetjeneste.plugins.configureMetrics
import no.nav.paw.bekreftelsetjeneste.routes.metricsRoutes
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstandSerde
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.Stores

fun main() {
    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)

    with(serverConfig) {
        embeddedServer(Netty, port = port) {
            module(applicationConfig)
        }.apply {
            addShutdownHook { stop(gracePeriodMillis, timeoutMillis) }
            start(wait = true)
        }
    }
}

fun Application.module(applicationConfig: ApplicationConfig) {
    val applicationContext = ApplicationContext.create(applicationConfig)

    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName),
                Serdes.UUID(),
                InternTilstandSerde()
            )
        )

    val kafkaStreams = buildKafkaStreams(
        applicationContext.applicationConfig,
        applicationContext.healthIndicatorRepository,
        streamsBuilder.appTopology(applicationContext)
    )


    configureMetrics(applicationContext)
    configureKafka(kafkaStreams)

    routing {
        healthRoutes(applicationContext.healthIndicatorRepository)
        metricsRoutes(applicationContext)
    }
}

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse = runBlocking {
    getIdAndKey(identitetsnummer)
}