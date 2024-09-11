package no.nav.paw.bekreftelse.api

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
import no.nav.paw.bekreftelse.api.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.plugins.configureAuthentication
import no.nav.paw.bekreftelse.api.plugins.configureHTTP
import no.nav.paw.bekreftelse.api.plugins.configureLogging
import no.nav.paw.bekreftelse.api.plugins.configureMetrics
import no.nav.paw.bekreftelse.api.plugins.configureOtel
import no.nav.paw.bekreftelse.api.plugins.configureSerialization
import no.nav.paw.bekreftelse.api.routes.healthRoutes
import no.nav.paw.bekreftelse.api.routes.bekreftelseRoutes
import no.nav.paw.bekreftelse.api.routes.swaggerRoutes
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
        bekreftelseRoutes(
            kafkaKeyClient = dependencies.kafkaKeysClient,
            bekreftelseStateStore = dependencies.bekreftelseStateStore,
            stateStoreName = applicationConfig.bekreftelseStateStoreName,
            kafkaStreams = dependencies.kafkaStreams,
            httpClient = dependencies.httpClient,
            bekreftelseProducer = dependencies.bekreftelseProducer,
            autorisasjonService = dependencies.autorisasjonService
        )
    }
}

