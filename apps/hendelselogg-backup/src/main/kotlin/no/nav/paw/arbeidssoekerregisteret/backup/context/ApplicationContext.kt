package no.nav.paw.arbeidssoekerregisteret.backup.context

import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.initClients
import no.nav.paw.arbeidssoekerregisteret.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.AzureConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.backup.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.m2mCfg
import no.nav.paw.arbeidssoekerregisteret.backup.database.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.backup.database.dataSource
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseHendelseRecordPostgresRepository
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import javax.sql.DataSource

data class ApplicationContext(
    val applicationConfig: ApplicationConfig,
    val azureConfig: AzureConfig,
    val serverConfig: ServerConfig,
    val securityConfig: SecurityConfig,
    val dataSource: DataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val hendelseKafkaConsumer: KafkaConsumer<Long, Hendelse>,
    val brukerstoetteService: BrukerstoetteService,
    val additionalMeterBinder: MeterBinder
) {
    companion object {
        fun create(): ApplicationContext {

            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application_config.toml")
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml")
            val azureConfig = loadNaisOrLocalConfiguration<AzureConfig>("azure_config.toml")
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>("security_config.toml")

            val dataSource = databaseConfig.dataSource()

            val (kafkaKeysClient, oppslagApiClient) = initClients(azureConfig.m2mCfg)
            val service = BrukerstoetteService(
                applicationConfig.version,
                kafkaKeysClient,
                oppslagApiClient,
                HendelseHendelseRecordPostgresRepository,
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

            return ApplicationContext(
                applicationConfig = applicationConfig,
                azureConfig = azureConfig,
                serverConfig = serverConfig,
                securityConfig = securityConfig,
                dataSource = dataSource,
                prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                hendelseKafkaConsumer = consumer,
                brukerstoetteService = service,
                additionalMeterBinder = KafkaClientMetrics(consumer)
            )
        }
    }
}
