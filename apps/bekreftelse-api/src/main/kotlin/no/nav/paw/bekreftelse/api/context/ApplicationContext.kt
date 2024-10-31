package no.nav.paw.bekreftelse.api.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.api.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.handler.KafkaConsumerExceptionHandler
import no.nav.paw.bekreftelse.api.producer.BekreftelseKafkaProducer
import no.nav.paw.bekreftelse.api.repository.BekreftelseRepository
import no.nav.paw.bekreftelse.api.services.AuthorizationService
import no.nav.paw.bekreftelse.api.services.BekreftelseService
import no.nav.paw.bekreftelse.api.utils.BekreftelseAvroSerializer
import no.nav.paw.bekreftelse.api.utils.createDataSource
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseDeserializer
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.LongSerializer
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val dataSource: DataSource,
    val kafkaKeysClient: KafkaKeysClient,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val bekreftelseKafkaProducer: Producer<Long, Bekreftelse>,
    val bekreftelseKafkaConsumer: KafkaConsumer<Long, BekreftelseHendelse>,
    val kafkaConsumerExceptionHandler: KafkaConsumerExceptionHandler,
    val authorizationService: AuthorizationService,
    val bekreftelseService: BekreftelseService
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)

            val dataSource = createDataSource(applicationConfig.database)

            val azureM2MTokenClient = azureAdM2MTokenClient(
                serverConfig.runtimeEnvironment, applicationConfig.azureM2M
            )

            val kafkaKeysClient = kafkaKeysClient(applicationConfig.kafkaKeysClient) {
                azureM2MTokenClient.createMachineToMachineToken(applicationConfig.kafkaKeysClient.scope)
            }

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val healthIndicatorRepository = HealthIndicatorRepository()

            val poaoTilgangClient = PoaoTilgangCachedClient(
                PoaoTilgangHttpClient(
                    baseUrl = applicationConfig.poaoClientConfig.url,
                    { azureM2MTokenClient.createMachineToMachineToken(applicationConfig.poaoClientConfig.scope) }
                )
            )

            val authorizationService = AuthorizationService(serverConfig, poaoTilgangClient)

            val kafkaConsumerExceptionHandler = KafkaConsumerExceptionHandler(
                healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator(HealthStatus.HEALTHY)),
                healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator(HealthStatus.HEALTHY))
            )

            val kafkaFactory = KafkaFactory(applicationConfig.kafkaClients)

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