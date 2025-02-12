package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.context

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.*
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.*
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.health.listener.withHealthIndicatorStateListener
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val kafkaStreamsList: List<KafkaStreams>
) {
    companion object {
        fun build(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
            val kafkaTopicsConfig = loadNaisOrLocalConfiguration<KafkaTopicsConfig>(KAFKA_TOPICS_CONFIG)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            val bekreftelseKafkaStreams = buildBekreftelseKafkaStreams(
                serverConfig = serverConfig,
                kafkaConfig = kafkaConfig,
                kafkaTopicsConfig = kafkaTopicsConfig,
                healthIndicatorRepository = healthIndicatorRepository,
            )
            val varselHendelseKafkaStreams = buildVarselHendelseKafkaStreams(
                kafkaConfig = kafkaConfig,
                kafkaTopicsConfig = kafkaTopicsConfig,
                meterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository
            )

            return ApplicationContext(
                serverConfig = serverConfig,
                prometheusMeterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                kafkaStreamsList = listOf(bekreftelseKafkaStreams, varselHendelseKafkaStreams)
            )
        }
    }
}

private fun buildBekreftelseKafkaStreams(
    serverConfig: ServerConfig,
    kafkaConfig: KafkaConfig,
    kafkaTopicsConfig: KafkaTopicsConfig,
    healthIndicatorRepository: HealthIndicatorRepository
): KafkaStreams {
    val kafkaTopology = StreamsBuilder()
        .internStateStore()
        .bekreftelseKafkaTopology(
            varselMeldingBygger = VarselMeldingBygger(
                runtimeEnvironment = serverConfig.runtimeEnvironment,
                minSideVarselKonfigurasjon = minSideVarselKonfigurasjon()
            ),
            kafkaTopicsConfig = kafkaTopicsConfig
        ).build()
    val kafkaStreamsFactory = KafkaStreamsFactory(BEKREFTELSE_STREAM_SUFFIX, kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
        .withExactlyOnce()
    return KafkaStreams(kafkaTopology, kafkaStreamsFactory.properties)
        .withApplicationTerminatingExceptionHandler()
        .withHealthIndicatorStateListener(
            livenessIndicator = healthIndicatorRepository
                .addLivenessIndicator(LivenessHealthIndicator(initialStatus = HealthStatus.UNKNOWN)),
            readinessIndicator = healthIndicatorRepository
                .addReadinessIndicator(ReadinessHealthIndicator(initialStatus = HealthStatus.UNKNOWN))
        )
}

private fun buildVarselHendelseKafkaStreams(
    kafkaConfig: KafkaConfig,
    kafkaTopicsConfig: KafkaTopicsConfig,
    meterRegistry: MeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository
): KafkaStreams {
    val kafkaTopology = StreamsBuilder()
        .varselHendelserKafkaTopology(kafkaTopicsConfig, meterRegistry)
        .build()
    val kafkaStreamsFactory = KafkaStreamsFactory(VARSEL_HENDELSE_STREAM_SUFFIX, kafkaConfig)
        .withDefaultKeySerde(Serdes.String()::class)
        .withDefaultValueSerde(VarselHendelseJsonSerde::class)
    return KafkaStreams(kafkaTopology, kafkaStreamsFactory.properties)
        .withApplicationTerminatingExceptionHandler()
        .withHealthIndicatorStateListener(
            livenessIndicator = healthIndicatorRepository
                .addLivenessIndicator(LivenessHealthIndicator(initialStatus = HealthStatus.UNKNOWN)),
            readinessIndicator = healthIndicatorRepository
                .addReadinessIndicator(ReadinessHealthIndicator(initialStatus = HealthStatus.UNKNOWN))
        )
}
