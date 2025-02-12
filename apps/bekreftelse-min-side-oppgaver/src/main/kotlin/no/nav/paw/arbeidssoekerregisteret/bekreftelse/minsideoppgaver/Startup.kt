package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.applicationTopology
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.stateStore
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KAFKA_TOPICS_CONFIG
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KafkaTopicsConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.minSideVarselKonfigurasjon
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.health.listener.withHealthIndicatorStateListener
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.slf4j.LoggerFactory

const val APP_SUFFIX = "beta-v2"

val appLogger = LoggerFactory.getLogger("main")
fun main() {
    appLogger.info("Starter...")
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val kafkaTopicsConfig = loadNaisOrLocalConfiguration<KafkaTopicsConfig>(KAFKA_TOPICS_CONFIG)

    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val healthIndicatorRepository = HealthIndicatorRepository()

    val kafkaTopology = StreamsBuilder()
        .stateStore()
        .applicationTopology(
            varselMeldingBygger = VarselMeldingBygger(
                runtimeEnvironment = currentRuntimeEnvironment,
                minSideVarselKonfigurasjon = minSideVarselKonfigurasjon()
            ),
            kafkaTopicsConfig = kafkaTopicsConfig
        )
    val streamsFactory = KafkaStreamsFactory(APP_SUFFIX, kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
        .withExactlyOnce()
    val stream = KafkaStreams(kafkaTopology, streamsFactory.properties)
        .withApplicationTerminatingExceptionHandler()
        .withHealthIndicatorStateListener(
            livenessIndicator = healthIndicatorRepository
                .addLivenessIndicator(LivenessHealthIndicator(initialStatus = HealthStatus.UNKNOWN)),
            readinessIndicator = healthIndicatorRepository
                .addReadinessIndicator(ReadinessHealthIndicator(initialStatus = HealthStatus.UNKNOWN))
        )

    appLogger.info("Starter KafkaStreams...")
    stream.start()
    appLogger.info("Starter Ktor...")
    embeddedServer(Netty, port = 8080) {
        configureMetrics(
            prometheusMeterRegistry,
            KafkaStreamsMetrics(stream)
        )
        routing {
            healthRoutes(healthIndicatorRepository)
        }
    }.start(wait = true)
    appLogger.info("Avslutter...")
}

fun Application.configureMetrics(
    registry: PrometheusMeterRegistry,
    vararg additionalBinders: MeterBinder
) {
    install(MicrometerMetrics) {
        this.registry = registry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics()
        ) + additionalBinders
    }
}
