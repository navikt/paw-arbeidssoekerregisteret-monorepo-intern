package no.nav.paw.bekreftelse.api.context

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.api.config.APPLICATION_CONFIG
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.SERVER_CONFIG
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.handler.KafkaConsumerExceptionHandler
import no.nav.paw.bekreftelse.api.producer.BekreftelseKafkaProducer
import no.nav.paw.bekreftelse.api.repository.BekreftelseRepository
import no.nav.paw.bekreftelse.api.services.AuthorizationService
import no.nav.paw.bekreftelse.api.services.BekreftelseService
import no.nav.paw.bekreftelse.api.utils.BekreftelseAvroSerializer
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseDeserializer
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.auth.AZURE_M2M_CONFIG
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.client.KAFKA_KEY_GENERATOR_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.tilgangskontroll.client.TILGANGSKONTROLL_CLIENT_CONFIG
import no.nav.paw.tilgangskontroll.client.TilgangskontrollClientConfig
import no.nav.paw.tilgangskontroll.client.tilgangsTjenesteForAnsatte
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.LongSerializer
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val dataSource: DataSource,
    val kafkaKeysClient: KafkaKeysClient,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val bekreftelseKafkaProducer: Producer<Long, Bekreftelse>,
    val bekreftelseKafkaConsumer: KafkaConsumer<Long, BekreftelseHendelse>,
    val kafkaConsumerExceptionHandler: KafkaConsumerExceptionHandler,
    val authorizationService: AuthorizationService,
    val bekreftelseService: BekreftelseService,
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
            val azureM2MConfig = loadNaisOrLocalConfiguration<AzureM2MConfig>(AZURE_M2M_CONFIG)
            val kafkaKeysClientConfig = loadNaisOrLocalConfiguration<KafkaKeyConfig>(KAFKA_KEY_GENERATOR_CLIENT_CONFIG)
            val tilgangskontrollClientConfig =
                loadNaisOrLocalConfiguration<TilgangskontrollClientConfig>(TILGANGSKONTROLL_CLIENT_CONFIG)

            val dataSource = createHikariDataSource(databaseConfig)

            val azureM2MTokenClient = azureAdM2MTokenClient(serverConfig.runtimeEnvironment, azureM2MConfig)

            val kafkaKeysClient = kafkaKeysClient(kafkaKeysClientConfig) {
                azureM2MTokenClient.createMachineToMachineToken(kafkaKeysClientConfig.scope)
            }

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val healthIndicatorRepository = HealthIndicatorRepository()

            val tilgangskontrollClient = tilgangsTjenesteForAnsatte(
                httpClient = HttpClient {
                    install(ContentNegotiation) {
                        jackson()
                    }
                },
                config = tilgangskontrollClientConfig,
                tokenProvider = { azureM2MTokenClient.createMachineToMachineToken(tilgangskontrollClientConfig.scope) }
            )

            val authorizationService = AuthorizationService(serverConfig, tilgangskontrollClient)

            val kafkaConsumerExceptionHandler = KafkaConsumerExceptionHandler(
                healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator(HealthStatus.HEALTHY)),
                healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator(HealthStatus.HEALTHY))
            )

            val kafkaFactory = KafkaFactory(kafkaConfig)

            val kafkaProducer = kafkaFactory.createProducer<Long, Bekreftelse>(
                clientId = applicationConfig.kafkaTopology.producerId,
                keySerializer = LongSerializer::class,
                valueSerializer = BekreftelseAvroSerializer::class
            )

            val kafkaConsumer = kafkaFactory.createConsumer(
                clientId = applicationConfig.kafkaTopology.consumerId,
                groupId = applicationConfig.kafkaTopology.consumerGroupId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = BekreftelseHendelseDeserializer::class,
                autoCommit = false
            )

            val bekreftelseKafkaProducer = BekreftelseKafkaProducer(applicationConfig, kafkaProducer)
            val bekreftelseRepository = BekreftelseRepository()

            val bekreftelseService = BekreftelseService(
                serverConfig,
                applicationConfig,
                prometheusMeterRegistry,
                kafkaKeysClient,
                bekreftelseKafkaProducer,
                bekreftelseRepository
            )

            return ApplicationContext(
                serverConfig,
                applicationConfig,
                securityConfig,
                dataSource,
                kafkaKeysClient,
                prometheusMeterRegistry,
                healthIndicatorRepository,
                kafkaProducer,
                kafkaConsumer,
                kafkaConsumerExceptionHandler,
                authorizationService,
                bekreftelseService
            )
        }
    }
}