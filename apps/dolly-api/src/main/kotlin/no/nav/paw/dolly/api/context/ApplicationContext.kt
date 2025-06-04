package no.nav.paw.dolly.api.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.dolly.api.client.oppslagClient
import no.nav.paw.dolly.api.config.APPLICATION_CONFIG
import no.nav.paw.dolly.api.config.ApplicationConfig
import no.nav.paw.dolly.api.config.SERVER_CONFIG
import no.nav.paw.dolly.api.config.ServerConfig
import no.nav.paw.dolly.api.producer.HendelseKafkaProducer
import no.nav.paw.dolly.api.service.DollyService
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.client.KAFKA_KEY_GENERATOR_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.LongSerializer

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val azureM2MConfig: AzureAdM2MConfig,
    val kafkaKeysClient: KafkaKeysClient,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val kafkaProducer: Producer<Long, Hendelse>,
    val dollyService: DollyService,
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
            val azureM2MConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
            val kafkaKeysClientConfig = loadNaisOrLocalConfiguration<KafkaKeyConfig>(KAFKA_KEY_GENERATOR_CLIENT_CONFIG)

            val azureM2MTokenClient = createAzureAdM2MTokenClient(serverConfig.runtimeEnvironment, azureM2MConfig)

            val kafkaKeysClient = kafkaKeysClient(kafkaKeysClientConfig) {
                azureM2MTokenClient.createMachineToMachineToken(kafkaKeysClientConfig.scope)
            }

            val oppslagClient = oppslagClient(applicationConfig.oppslagClientConfig) {
                azureM2MTokenClient.createMachineToMachineToken(applicationConfig.oppslagClientConfig.scope)
            }

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val kafkaFactory = KafkaFactory(kafkaConfig)

            val kafkaProducer = kafkaFactory.createProducer<Long, Hendelse>(
                clientId = applicationConfig.kafkaTopology.producerId,
                keySerializer = LongSerializer::class,
                valueSerializer = HendelseSerializer::class
            )

            val hendelseKafkaProducer = HendelseKafkaProducer(applicationConfig, kafkaProducer)

            val dollyService = DollyService(
                kafkaKeysClient,
                oppslagClient,
                hendelseKafkaProducer
            )

            return ApplicationContext(
                serverConfig,
                applicationConfig,
                securityConfig,
                azureM2MConfig,
                kafkaKeysClient,
                prometheusMeterRegistry,
                kafkaProducer,
                dollyService
            )
        }
    }
}