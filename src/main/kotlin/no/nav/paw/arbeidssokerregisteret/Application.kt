package no.nav.paw.arbeidssokerregisteret

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssokerregisteret.config.AuthProviders
import no.nav.paw.arbeidssokerregisteret.config.Config
import no.nav.paw.arbeidssokerregisteret.config.loadConfiguration
import no.nav.paw.arbeidssokerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureLogging
import no.nav.paw.arbeidssokerregisteret.plugins.configureMetrics
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes
import no.nav.paw.arbeidssokerregisteret.routes.healthRoutes
import no.nav.paw.arbeidssokerregisteret.routes.swaggerRoutes
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.config.kafka.KAFKA_CONFIG

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
    val applicationConfig = loadConfiguration<Config>()
    val dependencies = createDependencies(applicationConfig, KafkaFactory(kafkaConfig))
    val server = embeddedServer(Netty, port = 8080) {
        module(applicationConfig.authProviders, dependencies)
    }.start(wait = true)
    server.addShutdownHook {
        server.stop(300, 300)
    }
}

fun Application.module(authProviders: AuthProviders, dependencies: Dependencies) {
    // Konfigurerer plugins
    configureMetrics(dependencies.registry)
    configureHTTP()
    configureAuthentication(authProviders)
    configureLogging()
    configureSerialization()

    // Ruter
    routing {
        healthRoutes(dependencies.registry)
        swaggerRoutes()
        arbeidssokerRoutes(dependencies)
    }
}
