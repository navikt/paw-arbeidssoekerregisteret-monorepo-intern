package no.nav.paw.arbeidssoekerregisteret.backup

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
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SecurityConfig
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
    var dataSource: DataSource,
    val hendelseRecordRepository: HendelseRecordRepository,
    val brukerstoetteService: BrukerstoetteService,
    val metrics: Metrics,
    val backupService: BackupService,
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
            val dataSource = mockk<DataSource>(relaxed = true)
            val brukerstoetteService = BrukerstoetteService(
                applicationConfig.consumerVersion,
                kafkaKeysClient,
                oppslagApiClient,
                hendelseRecordRepository,
                HendelseDeserializer(),
            )
            val metrics = mockk<Metrics>(relaxed = true)
            val backupService = mockk<BackupService>(relaxed = true)

            return TestApplicationContext(
                applicationConfig = applicationConfig,
                serverConfig = serverConfig,
                securityConfig = securityConfig,
                kafkaKeysClient = kafkaKeysClient,
                oppslagApiClient = oppslagApiClient,
                hendelseRecordRepository = hendelseRecordRepository,
                databaseConfig = databaseConfig,
                dataSource = dataSource,
                brukerstoetteService = brukerstoetteService,
                metrics = metrics,
                backupService = backupService
            )
        }

        fun buildWithDatabase(): TestApplicationContext {
            val baseContext: TestApplicationContext = build()
            initDatabase()
            val dataSource = createHikariDataSource(baseContext.databaseConfig)
            val backupService = BackupService(HendelseRecordPostgresRepository, baseContext.metrics)
            return baseContext.copy(
                backupService = backupService,
                dataSource = dataSource,
            )
        }
    }
}

fun TestApplicationContext.toApplicationContext(): ApplicationContext =
    ApplicationContext(
        applicationConfig = applicationConfig,
        serverConfig = serverConfig,
        securityConfig = securityConfig,
        dataSource = dataSource,
        prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        hendelseKafkaConsumer = mockk(relaxed = true),
        brukerstoetteService = brukerstoetteService,
        additionalMeterBinder = mockk(relaxed = true),
        metrics = metrics,
        backupService = backupService
    )

fun initDatabase(): Database {
    val postgres = PostgreSQLContainer("postgres:17").apply {
        addEnv("POSTGRES_PASSWORD", "admin")
        addEnv("POSTGRES_USER", "admin")
        addEnv("POSTGRES_DATABASE", "hendelselogg_backup")
        addExposedPorts(5432)
    }

    postgres.start()
    postgres.waitingFor(Wait.forHealthcheck())
    val dbConfig = postgres.databaseConfig()
    val dataSource = createHikariDataSource(dbConfig)
    migrateDatabase(dataSource)
    return Database.connect(dataSource)
}

fun PostgreSQLContainer<*>.databaseConfig() =
    DatabaseConfig(
        host = host,
        port = firstMappedPort,
        username = username,
        password = password,
        database = databaseName,
        jdbcUrl = null,
    )
