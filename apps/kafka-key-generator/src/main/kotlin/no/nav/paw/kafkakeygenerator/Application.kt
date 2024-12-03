package no.nav.paw.kafkakeygenerator

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.config.AUTHENTICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.AuthenticationConfig
import no.nav.paw.kafkakeygenerator.config.DATABASE_CONFIG
import no.nav.paw.kafkakeygenerator.config.DatabaseConfig
import no.nav.paw.kafkakeygenerator.config.KAFKA_TOPOLOGY_CONFIG
import no.nav.paw.kafkakeygenerator.config.KafkaTopologyConfig
import no.nav.paw.kafkakeygenerator.config.PDL_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.config.PdlClientConfig
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import no.nav.paw.kafkakeygenerator.plugin.configSerialization
import no.nav.paw.kafkakeygenerator.plugin.configureAuthentication
import no.nav.paw.kafkakeygenerator.plugin.configureDatabase
import no.nav.paw.kafkakeygenerator.plugin.configureErrorHandling
import no.nav.paw.kafkakeygenerator.plugin.configureKafka
import no.nav.paw.kafkakeygenerator.plugin.configureLogging
import no.nav.paw.kafkakeygenerator.plugin.configureMetrics
import no.nav.paw.kafkakeygenerator.plugin.configureRouting
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.service.KafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.service.PdlService
import no.nav.paw.kafkakeygenerator.utils.createDataSource
import no.nav.paw.kafkakeygenerator.utils.createPdlClient
import no.nav.paw.pdl.PdlClient
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

fun main() {
    val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
    val authenticationConfig = loadNaisOrLocalConfiguration<AuthenticationConfig>(AUTHENTICATION_CONFIG)
    val azureAdM2MConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    val pdlClientConfig = loadNaisOrLocalConfiguration<PdlClientConfig>(PDL_CLIENT_CONFIG)
    val dataSource = createDataSource(databaseConfig)
    val pdlClient = createPdlClient(pdlClientConfig, azureAdM2MConfig)
    startApplication(authenticationConfig, dataSource, pdlClient)
}

fun startApplication(
    authenticationConfig: AuthenticationConfig,
    dataSource: DataSource,
    pdlClient: PdlClient
) {
    val database = Database.connect(dataSource)
    val healthIndicatorRepository = HealthIndicatorRepository()
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val identitetRepository = IdentitetRepository(database)
    val kafkaKeysRepository = KafkaKeysRepository(database)
    val kafkaKeysAuditRepository = KafkaKeysAuditRepository(database)
    val kafkaConsumerService = KafkaConsumerService(
        database,
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
        factory = Netty,
        port = 8080,
        configure = {
            connectionGroupSize = 8
            workerGroupSize = 8
            callGroupSize = 16
        }
    ) {
        configSerialization()
        configureLogging()
        configureErrorHandling()
        configureAuthentication(authenticationConfig)
        configureMetrics(
            meterRegistry = prometheusMeterRegistry,
            extraMeterBinders = listOf(KafkaClientMetrics(hendelseKafkaConsumer))
        )
        configureDatabase(dataSource)
        configureKafka(
            consumeFunction = kafkaConsumerService::handleRecords,
            errorFunction = kafkaConsumerService::handleException,
            kafkaConsumer = hendelseKafkaConsumer,
            kafkaTopics = listOf(kafkaTopologyConfig.hendelseloggTopic)
        )
        configureRouting(
            authenticationConfig,
            prometheusMeterRegistry,
            healthIndicatorRepository,
            kafkaKeysService,
            mergeDetector
        )
    }.start(wait = true)
}
