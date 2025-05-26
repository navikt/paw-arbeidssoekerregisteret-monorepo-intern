package no.nav.paw.arbeidssoekerregisteret.backup.context

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.initClients
import no.nav.paw.arbeidssoekerregisteret.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.backup.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseRecordPostgresRepository
import no.nav.paw.arbeidssoekerregisteret.backup.BackupService
import no.nav.paw.arbeidssoekerregisteret.backup.health.HealthIndicatorConsumerExceptionHandler
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.HwmRebalanceListener
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.NonCommittingKafkaConsumerWrapper
import no.nav.paw.arbeidssoekerregisteret.backup.metrics.Metrics
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.consumer.KafkaConsumerWrapper
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.auth.AZURE_M2M_CONFIG
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.common.serialization.LongDeserializer
import javax.sql.DataSource

data class ApplicationContext(
    val applicationConfig: ApplicationConfig,
    val serverConfig: ServerConfig,
    val securityConfig: SecurityConfig,
    val dataSource: DataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val hwmRebalanceListener: HwmRebalanceListener,
    val hendelseConsumerWrapper: KafkaConsumerWrapper<Long, Hendelse>,
    val brukerstoetteService: BrukerstoetteService,
    val additionalMeterBinder: MeterBinder,
    val metrics: Metrics,
    val backupService: BackupService,
) {
    companion object {
        fun create(): ApplicationContext {

            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application_config.toml")
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val azureM2MConfig = loadNaisOrLocalConfiguration<AzureM2MConfig>(AZURE_M2M_CONFIG)
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>("security_config.toml")

            val dataSource = createHikariDataSource(databaseConfig)

            val (kafkaKeysClient, oppslagApiClient) = initClients(azureM2MConfig)
            val service = BrukerstoetteService(
                applicationConfig.consumerVersion,
                kafkaKeysClient,
                oppslagApiClient,
                HendelseRecordPostgresRepository,
                hendelseDeserializer = HendelseDeserializer()
            )

            val consumer = KafkaFactory(kafkaConfig).createConsumer(
                groupId = applicationConfig.consumerGroupId,
                clientId = "client-${applicationConfig.consumerId}",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = HendelseDeserializer::class,
                autoCommit = false,
                autoOffsetReset = "earliest"
            )
            val hwmRebalanceListener = HwmRebalanceListener(applicationConfig.consumerVersion, consumer)
            val kafkaConsumerWrapper = NonCommittingKafkaConsumerWrapper(
                rebalanceListener = hwmRebalanceListener,
                topics = listOf(applicationConfig.hendelsesloggTopic),
                consumer = consumer,
                exceptionHandler = HealthIndicatorConsumerExceptionHandler(
                    LivenessHealthIndicator(),
                    ReadinessHealthIndicator()
                )
            )
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val metrics = Metrics(prometheusMeterRegistry)
            val backupService = BackupService(HendelseRecordPostgresRepository, metrics)


            return ApplicationContext(
                applicationConfig = applicationConfig,
                serverConfig = serverConfig,
                securityConfig = securityConfig,
                dataSource = dataSource,
                prometheusMeterRegistry = prometheusMeterRegistry,
                hwmRebalanceListener = hwmRebalanceListener,
                hendelseConsumerWrapper = kafkaConsumerWrapper,
                brukerstoetteService = service,
                additionalMeterBinder = KafkaClientMetrics(consumer),
                metrics = metrics,
                backupService = backupService,
            )
        }
    }
}
