package no.nav.paw.bekreftelsetjeneste

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.paw.bekreftelsetjeneste.ansvar.AnsvarSerde
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
import no.nav.paw.bekreftelsetjeneste.topology.buildTopology
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.route.healthRoutes
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import java.time.Instant

fun main() {
    val logger = LoggerFactory.getLogger("no.nav.paw.logger.application")

    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)

    logger.info("Starter: ${currentRuntimeEnvironment.appNameOrDefaultForLocal()}")

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
    val stream = StreamsBuilder()
    stream.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(applicationContext.applicationConfig.kafkaTopology.internStateStoreName),
            Serdes.UUID(),
            InternTilstandSerde()
        )
    )
    stream.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(applicationContext.applicationConfig.kafkaTopology.ansvarStateStoreName),
            Serdes.UUID(),
            AnsvarSerde()
        )
    )
    val kafkaTopology = stream.buildTopology(applicationContext)
    val kafkaStreams = buildKafkaStreams(applicationContext, kafkaTopology)

    configureMetrics(applicationContext)
    configureKafka(applicationContext, kafkaStreams)

    routing {
        healthRoutes(applicationContext.healthIndicatorRepository)
        metricsRoutes(applicationContext)
    }
}