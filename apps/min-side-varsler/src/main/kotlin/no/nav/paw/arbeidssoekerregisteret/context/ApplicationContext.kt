package no.nav.paw.arbeidssoekerregisteret.context

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.MIN_SIDE_VARSEL_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.model.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.repository.BestillingRepository
import no.nav.paw.arbeidssoekerregisteret.repository.BestiltVarselRepository
import no.nav.paw.arbeidssoekerregisteret.repository.EksterntVarselRepository
import no.nav.paw.arbeidssoekerregisteret.repository.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.repository.VarselRepository
import no.nav.paw.arbeidssoekerregisteret.service.BestillingService
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.topology.bekreftelseKafkaTopology
import no.nav.paw.arbeidssoekerregisteret.topology.periodeKafkaTopology
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
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val dataSource: DataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val varselService: VarselService,
    val bestillingService: BestillingService,
    val kafkaProducerList: List<Producer<*, *>>,
    val kafkaStreamsList: List<KafkaStreams>,
    val kafkaShutdownTimeout: Duration
) {
    companion object {
        fun build(): ApplicationContext {
            val logger = LoggerFactory.getLogger(ApplicationContext::class.java)
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
            val kafkaStreamsConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
            val minSideVarselConfig = loadNaisOrLocalConfiguration<MinSideVarselConfig>(MIN_SIDE_VARSEL_CONFIG)

            with(applicationConfig) {
                if (!periodeVarslerEnabled) logger.warn("Utsendelse av varsler ved avsluttet periode er deaktivert")
                if (!bekreftelseVarslerEnabled) logger.warn("Utsendelse av varsler ved tilgjengelig bekreftelse er deaktivert")
                if (!manuelleVarslerEnabled) logger.warn("Utsendelse av manuelle varsler er deaktivert")
            }

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            val dataSource = createHikariDataSource(databaseConfig)
            val periodeRepository = PeriodeRepository()
            val varselRepository = VarselRepository()
            val eksterntVarselRepository = EksterntVarselRepository()
            val bestillingRepository = BestillingRepository()
            val bestiltVarselRepository = BestiltVarselRepository()

            val varselMeldingBygger = VarselMeldingBygger(serverConfig.runtimeEnvironment, minSideVarselConfig)

            val kafkaFactory = KafkaFactory(kafkaConfig)
            val varselKafkaProducer = kafkaFactory.createProducer<String, String>(
                clientId = applicationConfig.varselProducerId,
                keySerializer = StringSerializer::class,
                valueSerializer = StringSerializer::class
            )

            val varselService = VarselService(
                applicationConfig = applicationConfig,
                meterRegistry = prometheusMeterRegistry,
                periodeRepository = periodeRepository,
                varselRepository = varselRepository,
                eksterntVarselRepository = eksterntVarselRepository,
                varselMeldingBygger = varselMeldingBygger
            )
            val bestillingService = BestillingService(
                applicationConfig = applicationConfig,
                meterRegistry = prometheusMeterRegistry,
                bestillingRepository = bestillingRepository,
                bestiltVarselRepository = bestiltVarselRepository,
                varselRepository = varselRepository,
                varselKafkaProducer = varselKafkaProducer,
                varselMeldingBygger = varselMeldingBygger
            )

            val periodeKafkaStreams = buildPeriodeKafkaStreams(
                applicationConfig = applicationConfig,
                kafkaConfig = kafkaStreamsConfig,
                meterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                varselService = varselService
            )
            val bekreftelseKafkaStreams = buildBekreftelseKafkaStreams(
                applicationConfig = applicationConfig,
                kafkaConfig = kafkaStreamsConfig,
                meterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                varselService = varselService
            )
            val varselHendelseKafkaStreams = buildVarselHendelseKafkaStreams(
                serverConfig = serverConfig,
                applicationConfig = applicationConfig,
                kafkaConfig = kafkaStreamsConfig,
                meterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                varselService = varselService
            )

            return ApplicationContext(
                serverConfig = serverConfig,
                applicationConfig = applicationConfig,
                securityConfig = securityConfig,
                dataSource = dataSource,
                prometheusMeterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                varselService = varselService,
                bestillingService = bestillingService,
                kafkaProducerList = listOf(varselKafkaProducer),
                kafkaStreamsList = listOf(
                    periodeKafkaStreams,
                    bekreftelseKafkaStreams,
                    varselHendelseKafkaStreams
                ),
                kafkaShutdownTimeout = applicationConfig.kafkaShutdownTimeout
            )
        }
    }
}

private fun buildPeriodeKafkaStreams(
    applicationConfig: ApplicationConfig,
    kafkaConfig: KafkaConfig,
    meterRegistry: MeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    varselService: VarselService
): KafkaStreams {
    val kafkaTopology = StreamsBuilder()
        .periodeKafkaTopology(
            applicationConfig = applicationConfig,
            meterRegistry = meterRegistry,
            varselService = varselService
        ).build()
    val kafkaStreamsFactory = KafkaStreamsFactory(applicationConfig.periodeStreamSuffix, kafkaConfig)
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

private fun buildBekreftelseKafkaStreams(
    applicationConfig: ApplicationConfig,
    kafkaConfig: KafkaConfig,
    meterRegistry: MeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    varselService: VarselService
): KafkaStreams {
    val kafkaTopology = StreamsBuilder()
        .bekreftelseKafkaTopology(
            applicationConfig = applicationConfig,
            meterRegistry = meterRegistry,
            varselService = varselService
        ).build()
    val kafkaStreamsFactory = KafkaStreamsFactory(applicationConfig.bekreftelseStreamSuffix, kafkaConfig)
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
    applicationConfig: ApplicationConfig,
    kafkaConfig: KafkaConfig,
    meterRegistry: MeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    varselService: VarselService
): KafkaStreams {
    val kafkaTopology = StreamsBuilder()
        .varselHendelserKafkaTopology(
            runtimeEnvironment = serverConfig.runtimeEnvironment,
            applicationConfig = applicationConfig,
            meterRegistry = meterRegistry,
            varselService = varselService
        )
        .build()
    val kafkaStreamsFactory = KafkaStreamsFactory(applicationConfig.varselHendelseStreamSuffix, kafkaConfig)
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
