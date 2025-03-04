package no.nav.paw.arbeidssoekerregisteret.context

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.KAFKA_TOPICS_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.KafkaTopologyConfig
import no.nav.paw.arbeidssoekerregisteret.config.MIN_SIDE_VARSEL_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.model.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.repository.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.repository.VarselRepository
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.topology.bekreftelseKafkaTopology
import no.nav.paw.arbeidssoekerregisteret.topology.varselHendelserKafkaTopology
import no.nav.paw.arbeidssoekerregisteret.utils.VarselHendelseJsonSerde
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
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
import java.time.Duration
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val dataSource: DataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val kafkaStreamsList: List<KafkaStreams>,
    val kafkaStreamsShutdownTimeout: Duration
) {
    companion object {
        fun build(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
            val kafkaTopicsConfig = loadNaisOrLocalConfiguration<KafkaTopologyConfig>(KAFKA_TOPICS_CONFIG)
            val minSideVarselConfig = loadNaisOrLocalConfiguration<MinSideVarselConfig>(MIN_SIDE_VARSEL_CONFIG)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            val dataSource = createHikariDataSource(databaseConfig)
            val periodeRepository = PeriodeRepository()
            val varselRepository = VarselRepository()

            val varselMeldingBygger = VarselMeldingBygger(serverConfig.runtimeEnvironment, minSideVarselConfig)

            val varselService = VarselService(periodeRepository, varselRepository, varselMeldingBygger)

            val bekreftelseKafkaStreams = buildBekreftelseKafkaStreams(
                serverConfig = serverConfig,
                kafkaConfig = kafkaConfig,
                kafkaTopicsConfig = kafkaTopicsConfig,
                meterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                varselService = varselService
            )
            /*val varselHendelseKafkaStreams = buildVarselHendelseKafkaStreams(
                serverConfig = serverConfig,
                kafkaConfig = kafkaConfig,
                kafkaTopologyConfig = kafkaTopicsConfig,
                meterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                varselService = varselService
            )*/ // TODO: Disable lesing av varsel-hendelser

            return ApplicationContext(
                serverConfig = serverConfig,
                dataSource = dataSource,
                prometheusMeterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                kafkaStreamsList = listOf(bekreftelseKafkaStreams),
                kafkaStreamsShutdownTimeout = kafkaTopicsConfig.shutdownTimeout
            )
        }
    }
}

private fun buildBekreftelseKafkaStreams(
    serverConfig: ServerConfig,
    kafkaConfig: KafkaConfig,
    kafkaTopicsConfig: KafkaTopologyConfig,
    meterRegistry: MeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    varselService: VarselService
): KafkaStreams {
    val kafkaTopology = StreamsBuilder()
        .bekreftelseKafkaTopology(
            runtimeEnvironment = serverConfig.runtimeEnvironment,
            kafkaTopicsConfig = kafkaTopicsConfig,
            meterRegistry = meterRegistry,
            varselService = varselService
        ).build()
    val kafkaStreamsFactory = KafkaStreamsFactory(kafkaTopicsConfig.bekreftelseStreamSuffix, kafkaConfig)
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
    serverConfig: ServerConfig,
    kafkaConfig: KafkaConfig,
    kafkaTopologyConfig: KafkaTopologyConfig,
    meterRegistry: MeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    varselService: VarselService
): KafkaStreams {
    val kafkaTopology = StreamsBuilder()
        .varselHendelserKafkaTopology(
            runtimeEnvironment = serverConfig.runtimeEnvironment,
            kafkaTopicsConfig = kafkaTopologyConfig,
            meterRegistry = meterRegistry,
            varselService = varselService
        )
        .build()
    val kafkaStreamsFactory = KafkaStreamsFactory(kafkaTopologyConfig.varselHendelseStreamSuffix, kafkaConfig)
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
