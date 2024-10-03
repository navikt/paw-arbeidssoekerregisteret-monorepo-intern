package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.applicationTopology
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.kafkaTopics
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.minSideVarselKonfigurasjon
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.StateStoreName
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.health.listener.withHealthIndicatorStateListener
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory

const val APP_SUFFIX = "beta-v2"
val STATE_STORE_NAME: StateStoreName = StateStoreName("internal_state")

val appLogger = LoggerFactory.getLogger("main")
fun main() {
    appLogger.info("Starter...")
    val kafkaTopics = kafkaTopics()
    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(STATE_STORE_NAME.value),
                Serdes.UUID(),
                jacksonSerde<InternTilstand>()
            )
        )
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val streamsFactory = KafkaStreamsFactory(APP_SUFFIX, kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
        .withExactlyOnce()
    val stream = KafkaStreams(streamsBuilder.applicationTopology(
        varselMeldingBygger = VarselMeldingBygger(
            runtimeEnvironment = currentRuntimeEnvironment,
            minSideVarselKonfigurasjon = minSideVarselKonfigurasjon()
        ),
        kafkaTopics = kafkaTopics,
        stateStoreName = STATE_STORE_NAME), streamsFactory.properties
    )
    stream.withApplicationTerminatingExceptionHandler()

    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val healthIndicatorRepository = HealthIndicatorRepository()
    val livenessHealthIndicator = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator(initialStatus = HealthStatus.UNKNOWN))
    val readinessHealthIndicator = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator(initialStatus = HealthStatus.UNKNOWN))
    stream.withHealthIndicatorStateListener(
        livenessIndicator = livenessHealthIndicator,
        readinessIndicator = readinessHealthIndicator
    )
    appLogger.info("Starter KafkaStreams...")
    stream.start()
    appLogger.info("Starter Ktor...")
    embeddedServer(Netty, port = 8080) {
        configureMetrics(
            registry,
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
