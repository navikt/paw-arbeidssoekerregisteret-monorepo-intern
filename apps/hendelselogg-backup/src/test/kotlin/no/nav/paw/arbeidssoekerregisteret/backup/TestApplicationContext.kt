package no.nav.paw.arbeidssoekerregisteret.backup

import com.zaxxer.hikari.HikariDataSource
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.OppslagApiClient
import no.nav.paw.arbeidssoekerregisteret.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseRecordPostgresRepository
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseRecordRepository
import no.nav.paw.arbeidssoekerregisteret.backup.database.migrateDatabase
import no.nav.paw.arbeidssoekerregisteret.backup.metrics.Metrics
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.kafka.consumer.NonCommittingKafkaConsumerWrapper
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.kafka.clients.consumer.Consumer
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

data class TestApplicationContext(
    val applicationConfig: ApplicationConfig,
    val serverConfig: ServerConfig,
    val securityConfig: SecurityConfig,
    val kafkaKeysClient: KafkaKeysClient,
    val oppslagApiClient: OppslagApiClient,
    val databaseConfig: DatabaseConfig,
    var dataSource: HikariDataSource,
    val hendelseRecordRepository: HendelseRecordRepository,
    val brukerstoetteService: BrukerstoetteService,
    val metrics: Metrics,
    val backupService: BackupService,
    val hendelseConsumer: Consumer<Long, Hendelse>,
    val hendelseConsumerWrapper: NonCommittingKafkaConsumerWrapper<Long, Hendelse>,
) {
    companion object {
        fun build(): TestApplicationContext {
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application_config.toml")
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val serverConfig = mockk<ServerConfig>(relaxed = true)
            val securityConfig = mockk<SecurityConfig>(relaxed = true)
            val kafkaKeysClient = mockk<KafkaKeysClient>(relaxed = true)
            val oppslagApiClient = mockk<OppslagApiClient>(relaxed = true)
            val hendelseRecordRepository = mockk<HendelseRecordRepository>(relaxed = true)
            val dataSource = mockk<HikariDataSource>(relaxed = true)
            val brukerstoetteService = BrukerstoetteService(
                applicationConfig.consumerVersion,
                kafkaKeysClient,
                oppslagApiClient,
                hendelseRecordRepository,
                HendelseDeserializer(),
            )
            val metrics = mockk<Metrics>(relaxed = true)
            val backupService = mockk<BackupService>(relaxed = true)
            val hendelseConsumer = mockk<Consumer<Long, Hendelse>>(relaxed = true)
            val hendelseConsumerWrapper = mockk<NonCommittingKafkaConsumerWrapper<Long, Hendelse>>(relaxed = true)

            return TestApplicationContext(
                applicationConfig = applicationConfig,
                serverConfig = serverConfig,
                securityConfig = securityConfig,
                kafkaKeysClient = kafkaKeysClient,
                oppslagApiClient = oppslagApiClient,
                databaseConfig = databaseConfig,
                dataSource = dataSource,
                hendelseRecordRepository = hendelseRecordRepository,
                brukerstoetteService = brukerstoetteService,
                metrics = metrics,
                backupService = backupService,
                hendelseConsumer = hendelseConsumer,
                hendelseConsumerWrapper = hendelseConsumerWrapper,
            )
        }

        fun buildWithDatabase(): TestApplicationContext {
            val baseContext: TestApplicationContext = build()
            val dataSource = createTestDataSource()
            initDatabase(dataSource)
            val backupService = BackupService(HendelseRecordPostgresRepository, baseContext.metrics)
            return baseContext.copy(
                backupService = backupService,
                dataSource = dataSource,
            )
        }
    }
}

fun TestApplicationContext.asApplicationContext(): ApplicationContext =
    ApplicationContext(
        applicationConfig = applicationConfig,
        serverConfig = serverConfig,
        securityConfig = securityConfig,
        dataSource = dataSource,
        prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        hwmRebalanceListener = mockk(relaxed = true),
        hendelseConsumerWrapper = hendelseConsumerWrapper,
        brukerstoetteService = brukerstoetteService,
        additionalMeterBinder = mockk(relaxed = true),
        metrics = metrics,
        backupService = backupService,
        hendelseConsumer = hendelseConsumer,
    )

fun initDatabase(dataSource: DataSource): Database {
    migrateDatabase(dataSource)
    return Database.connect(dataSource)
}

fun createTestDataSource(
    databaseConfig: DatabaseConfig = loadNaisOrLocalConfiguration(DATABASE_CONFIG),
    postgresContainer: PostgreSQLContainer<*> = postgresContainer(),
): HikariDataSource {
    val updatedDatabaseConfig = postgresContainer.let {
        databaseConfig.copy(
            host = it.host,
            port = it.firstMappedPort,
            username = it.username,
            password = it.password,
            database = it.databaseName
        )
    }
    return createHikariDataSource(updatedDatabaseConfig)
}

private fun postgresContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
    val postgres = PostgreSQLContainer("postgres:17").apply {
        addEnv("POSTGRES_PASSWORD", "hendelselogg_backup")
        addEnv("POSTGRES_USER", "Paw1234")
        addEnv("POSTGRES_DB", "hendelselogg_backup")
        addExposedPorts(5432)
    }
    postgres.start()
    postgres.waitingFor(Wait.forHealthcheck())
    return postgres
}
