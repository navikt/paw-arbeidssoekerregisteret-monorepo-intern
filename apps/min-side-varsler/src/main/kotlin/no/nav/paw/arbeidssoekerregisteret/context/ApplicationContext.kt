package no.nav.paw.arbeidssoekerregisteret.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.MIN_SIDE_VARSEL_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.model.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.service.BestillingService
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.topology.buildBekreftelseTopology
import no.nav.paw.arbeidssoekerregisteret.topology.buildPeriodeTopology
import no.nav.paw.arbeidssoekerregisteret.topology.buildVarselHendelseTopology
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
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
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
            val kafkaStreamsConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
            val minSideVarselConfig = loadNaisOrLocalConfiguration<MinSideVarselConfig>(MIN_SIDE_VARSEL_CONFIG)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            val dataSource = createHikariDataSource(databaseConfig)

            val varselMeldingBygger = VarselMeldingBygger(serverConfig.runtimeEnvironment, minSideVarselConfig)

            val kafkaFactory = KafkaFactory(kafkaConfig)
            val varselKafkaProducer = kafkaFactory.createProducer<String, String>(
                clientId = applicationConfig.varselProducerId,
                keySerializer = StringSerializer::class,
                valueSerializer = StringSerializer::class
            )

            val varselService = VarselService(
                meterRegistry = prometheusMeterRegistry,
                varselMeldingBygger = varselMeldingBygger
            )
            val bestillingService = BestillingService(
                applicationConfig = applicationConfig,
                meterRegistry = prometheusMeterRegistry,
                varselKafkaProducer = varselKafkaProducer,
                varselMeldingBygger = varselMeldingBygger
            )

            val periodeKafkaStreams = buildPeriodeTopology(
                applicationConfig = applicationConfig,
                kafkaConfig = kafkaStreamsConfig,
                meterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                varselService = varselService
            )
            val bekreftelseKafkaStreams = buildBekreftelseTopology(
                applicationConfig = applicationConfig,
                kafkaConfig = kafkaStreamsConfig,
                meterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                varselService = varselService
            )
            val varselHendelseKafkaStreams = buildVarselHendelseTopology(
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
