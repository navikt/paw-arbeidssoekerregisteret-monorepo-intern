package no.nav.paw.arbeidssoekerregisteret.backup

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.OppslagApiClient
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.brukerstoetteRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.customExceptionResolver
import no.nav.paw.arbeidssoekerregisteret.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.database.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.backup.database.dataSource
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseRecordRepository
import no.nav.paw.arbeidssoekerregisteret.backup.database.migrateDatabase
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.jackson.configureJackson
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait

val testApplicationContext = ApplicationContext(
    hendelseKafkaConsumer = mockk(relaxed = true),
    applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application_config.toml"),
    serverConfig = loadNaisOrLocalConfiguration<ServerConfig>("server_config.toml"),
    securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>("security_config.toml"),
    dataSource = loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml").dataSource(),
    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    brukerstoetteService = mockk(relaxed = true),
    additionalMeterBinder = mockk(relaxed = true),
)

open class TestApplicationContext(
    open val applicationConfig: ApplicationConfig,
    open val serverConfig: ServerConfig,
    open val securityConfig: SecurityConfig,
    open val kafkaKeysClient: KafkaKeysClient,
    open val oppslagApiClient: OppslagApiClient,
    open val hendelseRecordRepository: HendelseRecordRepository,
    open val brukerstoetteService: BrukerstoetteService,
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
                applicationConfig.version,
                kafkaKeysClient,
                oppslagApiClient,
                hendelseRecordRepository,
                HendelseDeserializer()
            )
            return TestApplicationContext(
                applicationConfig = applicationConfig,
                serverConfig = serverConfig,
                securityConfig = securityConfig,
                kafkaKeysClient = kafkaKeysClient,
                oppslagApiClient = oppslagApiClient,
                hendelseRecordRepository = hendelseRecordRepository,
                brukerstoetteService = brukerstoetteService
            )
        }
    }

    fun initDatabase(): Database {
        val databaseName = "backupTest"
        val postgres = PostgreSQLContainer("postgres:17")
            .withDatabaseName(databaseName)
            .withUsername("postgres")
            .withPassword("postgres")

        postgres.start()
        postgres.waitingFor(Wait.forHealthcheck())
        val dbConfig = postgres.databaseConfig()
        val dataSource = dbConfig.dataSource()
        migrateDatabase(dataSource)
        return Database.connect(dataSource)
    }

    private fun brukerstoetteService(): BrukerstoetteService {
        return BrukerstoetteService(
            applicationConfig.version,
            kafkaKeysClient,
            oppslagApiClient,
            hendelseRecordRepository,
            HendelseDeserializer()
        )
    }

    private fun createApplicationContext(service: BrukerstoetteService = brukerstoetteService()) = ApplicationContext(
        applicationConfig = applicationConfig,
        serverConfig = serverConfig,
        securityConfig = securityConfig,
        dataSource = loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml").dataSource(),
        prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        hendelseKafkaConsumer = mockk(relaxed = true),
        brukerstoetteService = service,
        additionalMeterBinder = mockk(relaxed = true)
    )

    fun ApplicationTestBuilder.configureTestApplication(service: BrukerstoetteService = brukerstoetteService()) {
        with(createApplicationContext(service)) {
            application {
                installContentNegotiationPlugin()
                installErrorHandlingPlugin(
                    customResolver = customExceptionResolver()
                )
                installAuthenticationPlugin(securityConfig.authProviders)
                install(IgnoreTrailingSlash)
                routing {
                    route("/api/v1") {
                        brukerstoetteRoutes(brukerstoetteService)
                    }
                }
            }
        }
    }

    fun ApplicationTestBuilder.configureTestClient(): HttpClient {
        return createClient {
            install(ContentNegotiation) {
                jackson {
                    configureJackson()
                }
            }
        }
    }

}

fun PostgreSQLContainer<*>.databaseConfig(): DatabaseConfig {
    return DatabaseConfig(
        host = host,
        port = firstMappedPort,
        username = username,
        password = password,
        name = databaseName,
        jdbc = null
    )
}
