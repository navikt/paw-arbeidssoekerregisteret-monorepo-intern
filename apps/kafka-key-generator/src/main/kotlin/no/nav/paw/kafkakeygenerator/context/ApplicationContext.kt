package no.nav.paw.kafkakeygenerator.context

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.kafkakeygenerator.config.KAFKA_TOPOLOGY_CONFIG
import no.nav.paw.kafkakeygenerator.config.KafkaTopologyConfig
import no.nav.paw.kafkakeygenerator.config.PDL_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.config.PdlClientConfig
import no.nav.paw.kafkakeygenerator.config.SERVER_CONFIG
import no.nav.paw.kafkakeygenerator.config.ServerConfig
import no.nav.paw.kafkakeygenerator.handler.HealthIndicatorConsumerExceptionHandler
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.service.PawHendelseKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PdlService
import no.nav.paw.kafkakeygenerator.utils.createPdlClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val securityConfig: SecurityConfig,
    val kafkaTopologyConfig: KafkaTopologyConfig,
    val dataSource: DataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val pawHendelseKafkaConsumer: KafkaConsumer<Long, Hendelse>,
    val pawHendelseKafkaConsumerService: PawHendelseKafkaConsumerService,
    val pawHendelseConsumerExceptionHandler: ConsumerExceptionHandler,
    val kafkaKeysService: KafkaKeysService,
    val mergeDetector: MergeDetector,
    val additionalMeterBinders: List<MeterBinder>
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
            val kafkaTopologyConfig = loadNaisOrLocalConfiguration<KafkaTopologyConfig>(KAFKA_TOPOLOGY_CONFIG)
            val azureAdM2MConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
            val pdlClientConfig = loadNaisOrLocalConfiguration<PdlClientConfig>(PDL_CLIENT_CONFIG)
            val dataSource = createHikariDataSource(databaseConfig)
            val pdlClient = createPdlClient(pdlClientConfig, azureAdM2MConfig)
            val healthIndicatorRepository = HealthIndicatorRepository()
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val identitetRepository = IdentitetRepository()
            val kafkaKeysRepository = KafkaKeysRepository()
            val kafkaKeysAuditRepository = KafkaKeysAuditRepository()
            val pawHendelseKafkaConsumerService = PawHendelseKafkaConsumerService(
                meterRegistry = prometheusMeterRegistry,
                identitetRepository = identitetRepository,
                kafkaKeysRepository = kafkaKeysRepository,
                kafkaKeysAuditRepository = kafkaKeysAuditRepository
            )
            val pawHendelseConsumerExceptionHandler = HealthIndicatorConsumerExceptionHandler(
                livenessIndicator = healthIndicatorRepository.livenessIndicator(defaultStatus = HealthStatus.HEALTHY),
                readinessIndicator = healthIndicatorRepository.readinessIndicator(defaultStatus = HealthStatus.HEALTHY)
            )
            val pdlService = PdlService(pdlClient = pdlClient)
            val kafkaKeysService = KafkaKeysService(
                meterRegistry = prometheusMeterRegistry,
                kafkaKeysRepository = kafkaKeysRepository,
                pdlService = pdlService
            )
            val mergeDetector = MergeDetector(
                kafkaKeysRepository = kafkaKeysRepository,
                pdlService = pdlService
            )
            val kafkaFactory = KafkaFactory(kafkaConfig)
            val hendelseKafkaConsumer = kafkaFactory.createConsumer(
                groupId = kafkaTopologyConfig.consumerGroupId,
                clientId = "${kafkaTopologyConfig.consumerGroupId}-consumer",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = HendelseDeserializer::class
            )
            return ApplicationContext(
                serverConfig = serverConfig,
                securityConfig = securityConfig,
                kafkaTopologyConfig = kafkaTopologyConfig,
                dataSource = dataSource,
                prometheusMeterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                pawHendelseKafkaConsumer = hendelseKafkaConsumer,
                pawHendelseKafkaConsumerService = pawHendelseKafkaConsumerService,
                pawHendelseConsumerExceptionHandler = pawHendelseConsumerExceptionHandler,
                kafkaKeysService = kafkaKeysService,
                mergeDetector = mergeDetector,
                additionalMeterBinders = listOf(KafkaClientMetrics(hendelseKafkaConsumer))
            )
        }
    }
}
