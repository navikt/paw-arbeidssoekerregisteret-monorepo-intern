package no.nav.paw.arbeidssoekerregisteret.backup.utils

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
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.HendelseloggBackup
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

data class TestApplicationContext(
    val applicationConfig: ApplicationConfig,
    val serverConfig: ServerConfig,
    val securityConfig: SecurityConfig,
    val kafkaKeysClient: KafkaKeysClient,
    val oppslagApiClient: OppslagApiClient,
    val hendelseRecordRepository: HendelseRecordRepository,
    val brukerstoetteService: BrukerstoetteService,
    val metrics: Metrics,
    val hendelseloggBackup: HendelseloggBackup,
) {
    companion object {
        fun build(): TestApplicationContext {
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application_config.toml")
            val serverConfig = mockk<ServerConfig>(relaxed = true)
            val securityConfig = mockk<SecurityConfig>(relaxed = true)
            val kafkaKeysClient = mockk<KafkaKeysClient>()
            val oppslagApiClient = mockk<OppslagApiClient>()
            val hendelseRecordRepository = mockk<HendelseRecordRepository>(relaxed = true)
            val brukerstoetteService = BrukerstoetteService(
                applicationConfig.consumerVersion,
                kafkaKeysClient,
                oppslagApiClient,
                HendelseRecordPostgresRepository,
                HendelseDeserializer(),
            )
            val metrics = mockk<Metrics>(relaxed = true)
            val hendelseloggBackup = mockk<HendelseloggBackup>(relaxed = true)

            return TestApplicationContext(
                applicationConfig = applicationConfig,
                serverConfig = serverConfig,
                securityConfig = securityConfig,
                kafkaKeysClient = kafkaKeysClient,
                oppslagApiClient = oppslagApiClient,
                hendelseRecordRepository = hendelseRecordRepository,
                brukerstoetteService = brukerstoetteService,
                metrics = metrics,
                hendelseloggBackup = hendelseloggBackup
            )
        }

        fun buildWithDatabase(): TestApplicationContext {
            initDatabase()
            val baseContext: TestApplicationContext = build()
            val hendelseloggBackup = HendelseloggBackup(HendelseRecordPostgresRepository, baseContext.metrics)
            return baseContext.copy(
                hendelseloggBackup = hendelseloggBackup
            )
        }
    }
}

fun TestApplicationContext.toApplicationContext() =
    ApplicationContext(
        applicationConfig = applicationConfig,
        serverConfig = serverConfig,
        securityConfig = securityConfig,
        dataSource = createHikariDataSource(loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)),
        prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        hendelseKafkaConsumer = mockk(relaxed = true),
        brukerstoetteService = brukerstoetteService,
        additionalMeterBinder = mockk(relaxed = true),
        metrics = metrics,
        hendelseloggBackup = hendelseloggBackup
    )

fun initDatabase(): Database {
    val databaseName = "backupTest"
    val postgres = PostgreSQLContainer("postgres:17")
        .withDatabaseName(databaseName)
        .withUsername("postgres")
        .withPassword("postgres")

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
    )
