package no.nav.paw.rapportering.api

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.rapportering.api.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.rapportering.api.config.ApplicationConfig
import no.nav.paw.rapportering.api.plugins.configureAuthentication
import no.nav.paw.rapportering.api.plugins.configureHTTP
import no.nav.paw.rapportering.api.plugins.configureLogging
import no.nav.paw.rapportering.api.plugins.configureMetrics
import no.nav.paw.rapportering.api.plugins.configureOtel
import no.nav.paw.rapportering.api.plugins.configureSerialization
import no.nav.paw.rapportering.api.routes.healthRoutes
import no.nav.paw.rapportering.api.routes.rapporteringRoutes
import no.nav.paw.rapportering.api.routes.swaggerRoutes
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("rapportering-api")
    logger.info("Starter: ${ApplicationInfo.id}")

    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val kafkaStreamsConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val azureM2MConfig = loadNaisOrLocalConfiguration<AzureM2MConfig>("azure_m2m_key_config.toml")
    val kafkaKeyConfig = loadNaisOrLocalConfiguration<KafkaKeyConfig>("kafka_key_generator_client_config.toml")

    val dependencies = createDependencies(
        applicationConfig,
        kafkaConfig,
        kafkaStreamsConfig,
        azureM2MConfig,
        kafkaKeyConfig
    )

    embeddedServer(Netty, port = 8080) {
        module(applicationConfig, dependencies)
    }.apply {
        addShutdownHook { stop(300, 300) }
        start(wait = true)
    }
}

fun Application.module(
    applicationConfig: ApplicationConfig,
    dependencies: Dependencies
) {
    configureMetrics(dependencies.prometheusMeterRegistry)
    configureHTTP()
    configureAuthentication(applicationConfig.authProviders)
    configureLogging()
    configureSerialization()
    configureOtel()

    routing {
        healthRoutes(dependencies.prometheusMeterRegistry, dependencies.health)
        swaggerRoutes()
        rapporteringRoutes(
            kafkaKeyClient = dependencies.kafkaKeyClient,
            rapporteringStateStore = dependencies.rapporteringStateStore,
            rapporteringStateStoreName = applicationConfig.rapporteringStateStoreName,
            kafkaStreams = dependencies.kafkaStreams,
            httpClient = dependencies.httpClient,
            rapporteringProducer = dependencies.rapporteringProducer,
            autorisasjonService = dependencies.autorisasjonService
        )
    }
}

