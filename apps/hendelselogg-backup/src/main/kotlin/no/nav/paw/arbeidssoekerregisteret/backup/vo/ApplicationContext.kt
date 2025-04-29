package no.nav.paw.arbeidssoekerregisteret.backup.vo

import io.ktor.server.application.ServerConfig
import io.ktor.server.config.ApplicationConfig
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.AzureConfig
import no.nav.paw.arbeidssoekerregisteret.backup.CONSUMER_GROUP
import no.nav.paw.arbeidssoekerregisteret.backup.CURRENT_VERSION
import no.nav.paw.arbeidssoekerregisteret.backup.database.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.backup.database.dataSource
import no.nav.paw.arbeidssoekerregisteret.backup.database.migrateDatabase
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SecurityConfig
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.sql.Database
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

@JvmRecord
data class ApplicationContextOld(
    val consumerVersion: Int,
    val logger: Logger,
    val meterRegistry: PrometheusMeterRegistry,
    val shutdownCalled: AtomicBoolean = AtomicBoolean(false),
    val azureConfig: AzureConfig,
)

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val securityConfig: SecurityConfig,
    val dataSource: DataSource,
    val kafkaKeysClient: KafkaKeysClient,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val hendelseKafkaConsumer: KafkaConsumer<Long, Hendelse>,
    val healthIndicatorConsumerExceptionHandler: HealthIndicatorConsumerExceptionHandler,
    val authorizationService: AuthorizationService,
    val bekreftelseService: BekreftelseService,
    val additionalMeterBinders: List<MeterBinder>,
) {

    companion object {
        fun create(): ApplicationContext {
            val logger = LoggerFactory.getLogger("backup-context-create")
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml")
            val ds = databaseConfig.dataSource()
            logger.info("Connection to database($this)...")
            Database.Companion.connect(ds)
            logger.info("Migrating database...")
            migrateDatabase(ds)
            logger.info("Connection to kafka...")
            val consumer = KafkaFactory(kafkaConfig).createConsumer(
                groupId = CONSUMER_GROUP,
                clientId = "client-$CONSUMER_GROUP",
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = HendelseDeserializer::class,
                autoCommit = false,
                autoOffsetReset = "earliest"
            )
            return ApplicationContext(
                logger = LoggerFactory.getLogger("backup-context"),
                consumerVersion = CURRENT_VERSION,
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                shutdownCalled = AtomicBoolean(false),
                azureConfig = loadNaisOrLocalConfiguration("azure.toml"),
                hendelseKafkaConsumer = consumer,
            )
        }
    }
}
