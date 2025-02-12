package no.nav.paw.kafkakeygenerator

import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.database.plugin.installDatabasePlugin
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.config.KAFKA_TOPOLOGY_CONFIG
import no.nav.paw.kafkakeygenerator.config.KafkaTopologyConfig
import no.nav.paw.kafkakeygenerator.config.PDL_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.config.PdlClientConfig
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import no.nav.paw.kafkakeygenerator.plugin.configureRouting
import no.nav.paw.kafkakeygenerator.plugin.installCustomLoggingPlugin
import no.nav.paw.kafkakeygenerator.plugin.installKafkaPlugins
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.service.KafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.service.PdlService
import no.nav.paw.kafkakeygenerator.utils.createPdlClient
import no.nav.paw.metrics.plugin.installWebAppMetricsPlugin
import no.nav.paw.pdl.PdlClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.apache.kafka.common.serialization.LongDeserializer
import javax.sql.DataSource

fun main() {
    val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
    val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    val azureAdM2MConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    val pdlClientConfig = loadNaisOrLocalConfiguration<PdlClientConfig>(PDL_CLIENT_CONFIG)
    val dataSource = createHikariDataSource(databaseConfig)
    val pdlClient = createPdlClient(pdlClientConfig, azureAdM2MConfig)
    startApplication(securityConfig, dataSource, pdlClient)
}

fun startApplication(securityConfig: SecurityConfig, dataSource: DataSource, pdlClient: PdlClient) {
    val healthIndicatorRepository = HealthIndicatorRepository()
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val identitetRepository = IdentitetRepository()
    val kafkaKeysRepository = KafkaKeysRepository()
    val kafkaKeysAuditRepository = KafkaKeysAuditRepository()
    val kafkaConsumerService = KafkaConsumerService(
        healthIndicatorRepository,
        prometheusMeterRegistry,
        identitetRepository,
        kafkaKeysRepository,
        kafkaKeysAuditRepository
    )
    val pdlService = PdlService(pdlClient)
    val kafkaKeysService = KafkaKeysService(
        prometheusMeterRegistry,
        kafkaKeysRepository,
        pdlService
    )
    val mergeDetector = MergeDetector(
        pdlService,
        kafkaKeysRepository
    )
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
    val kafkaTopologyConfig = loadNaisOrLocalConfiguration<KafkaTopologyConfig>(KAFKA_TOPOLOGY_CONFIG)
    val kafkaFactory = KafkaFactory(kafkaConfig)

    val hendelseKafkaConsumer = kafkaFactory.createConsumer(
        groupId = kafkaTopologyConfig.consumerGroupId,
        clientId = "${kafkaTopologyConfig.consumerGroupId}-consumer",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = HendelseDeserializer::class
    )

    embeddedServer(
        Netty,
        configure = {
            connectionGroupSize = 8
            workerGroupSize = 8
            callGroupSize = 16
            connectors.add(EngineConnectorBuilder().apply {
                port = 8080
            })
        }
    ) {
        installErrorHandlingPlugin()
        installContentNegotiationPlugin()
        installCustomLoggingPlugin()
        installAuthenticationPlugin(securityConfig.authProviders)
        installWebAppMetricsPlugin(
            meterRegistry = prometheusMeterRegistry,
            additionalMeterBinders = listOf(KafkaClientMetrics(hendelseKafkaConsumer))
        )
        installDatabasePlugin(dataSource)
        installKafkaPlugins(
            kafkaTopologyConfig = kafkaTopologyConfig,
            kafkaConsumer = hendelseKafkaConsumer,
            kafkaConsumerService = kafkaConsumerService
        )
        configureRouting(
            prometheusMeterRegistry,
            healthIndicatorRepository,
            kafkaKeysService,
            mergeDetector
        )
    }.start(wait = true)
}
