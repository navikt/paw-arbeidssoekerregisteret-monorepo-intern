package no.nav.paw.arbeidssokerregisteret

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.application.InngangsReglerV2
import no.nav.paw.arbeidssokerregisteret.application.OpplysningerRequestHandler
import no.nav.paw.arbeidssokerregisteret.application.StartStoppRequestHandler
import no.nav.paw.arbeidssokerregisteret.config.AuthProviders
import no.nav.paw.arbeidssokerregisteret.config.CONFIG_FILE_NAME
import no.nav.paw.arbeidssokerregisteret.config.Config
import no.nav.paw.arbeidssokerregisteret.plugins.*
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutesV2
import no.nav.paw.arbeidssokerregisteret.routes.healthRoutes
import no.nav.paw.arbeidssokerregisteret.routes.swaggerRoutes
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("app")
    logger.info("Starter ${ApplicationInfo.id}")
    val applicationConfig = loadNaisOrLocalConfiguration<Config>(CONFIG_FILE_NAME)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val (startStoppRequestHandler, opplysningerRequestHandler) = requestHandlers(
        config = applicationConfig,
        kafkaFactory = KafkaFactory(kafkaConfig),
        regler = InngangsReglerV2,
        registry = registry
    )
    val server = embeddedServer(
        factory = Netty,
        configure = {
            connectionGroupSize = 8
            workerGroupSize = 8
            callGroupSize = 16
            connectors.add(EngineConnectorBuilder().apply {
                port = 8080
            })
        },
    ) {
        module(
            registry = registry,
            authProviders = applicationConfig.authProviders,
            startStoppRequestHandler = startStoppRequestHandler,
            opplysningerRequestHandler = opplysningerRequestHandler
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
    startStoppRequestHandler: StartStoppRequestHandler,
    opplysningerRequestHandler: OpplysningerRequestHandler
) {
    configureMetrics(registry)
    configureHTTP()
    configureAuthentication(authProviders)
    configureLogging()
    configureSerialization()
    install(OtelTraceIdPlugin)

    routing {
        healthRoutes(registry)
        swaggerRoutes()
        authenticate("tokenx", "azure") {
            arbeidssokerRoutes(opplysningerRequestHandler)
            arbeidssokerRoutesV2(startStoppRequestHandler)
        }
    }
}

val OtelTraceIdPlugin =
    createApplicationPlugin("OtelTraceIdPlugin") {
        onCallRespond { call, _ ->
            runCatching { Span.current().spanContext.traceId }
                .onSuccess { call.response.headers.append("x-trace-id", it) }
        }
    }
