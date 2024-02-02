package no.nav.paw.arbeidssokerregisteret

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.application.RequestHandler
import no.nav.paw.arbeidssokerregisteret.config.AuthProviders
import no.nav.paw.arbeidssokerregisteret.config.Config
import no.nav.paw.arbeidssokerregisteret.config.loadConfiguration
import no.nav.paw.arbeidssokerregisteret.plugins.*
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes
import no.nav.paw.arbeidssokerregisteret.routes.healthRoutes
import no.nav.paw.arbeidssokerregisteret.routes.swaggerRoutes
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory

fun main() {

    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
    val applicationConfig = loadConfiguration<Config>()
    val requestHandler = requestHandler(applicationConfig, KafkaFactory(kafkaConfig))

    val server = embeddedServer(Netty, port = 8080) {
        module(
            registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            authProviders = applicationConfig.authProviders,
            requestHandler = requestHandler
        )
    }
    server.addShutdownHook {
        server.stop(300, 300)
    }
    server.start(wait = true)
}

fun Application.module(
    registry: PrometheusMeterRegistry,
    authProviders: AuthProviders,
    requestHandler: RequestHandler
) {
    configureMetrics(registry)
    configureHTTP()
    configureAuthentication(authProviders)
    configureLogging()
    configureSerialization()

    // Ruter
    routing {
        healthRoutes(registry)
        swaggerRoutes()
        arbeidssokerRoutes(requestHandler)
    }
}
