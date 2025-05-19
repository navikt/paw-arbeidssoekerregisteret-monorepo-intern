package no.nav.paw.arbeidssoekerregisteret.backup

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.OppslagApiClient
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.brukerstoetteRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.customExceptionResolver
import no.nav.paw.arbeidssoekerregisteret.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.AzureConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.database.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.backup.database.dataSource
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseRecordRepository
import no.nav.paw.arbeidssoekerregisteret.backup.database.migrateDatabase
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.jackson.configureJackson
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait

fun testApplicationContext(
    brukerstoetteService: BrukerstoetteService = mockk(relaxed = true),
): ApplicationContext {
    return testApplicationContext.copy(
        brukerstoetteService = brukerstoetteService,
    )
}

val testApplicationContext = ApplicationContext(
    hendelseKafkaConsumer = mockk(relaxed = true),
    azureConfig = loadNaisOrLocalConfiguration<AzureConfig>("azure_config.toml"),
    applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application_config.toml"),
    serverConfig = loadNaisOrLocalConfiguration<ServerConfig>("server_config.toml"),
    securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>("security_config.toml"),
    dataSource = loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml").dataSource(),
    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    brukerstoetteService = mockk(relaxed = true),
    additionalMeterBinder = mockk(relaxed = true),
)

open class TestApplicationContext(
    open val hendelseKafkaConsumer: KafkaConsumer<Long, Hendelse>,
    open val azureConfig: AzureConfig,
    open val applicationConfig: ApplicationConfig,
    open val serverConfig: ServerConfig,
    open val securityConfig: SecurityConfig,
    open val prometheusMeterRegistry: PrometheusMeterRegistry,
    open val kafkaKeysClient: KafkaKeysClient,
    open val oppslagApiClient: OppslagApiClient,
    open val hendelseRecordRepository: HendelseRecordRepository,
    open val brukerstoetteService: BrukerstoetteService,
    open val additionalMeterBinder: MeterBinder
) {
    companion object {
        fun build(): TestApplicationContext {
            val hendelseKafkaConsumer = mockk<KafkaConsumer<Long, Hendelse>>(relaxed = true)
            val azureConfig = loadNaisOrLocalConfiguration<AzureConfig>("azure_config.toml")
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application_config.toml")
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>("server_config.toml")
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>("security_config.toml")
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
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
            val additionalMeterBinder = mockk<MeterBinder>(relaxed = true)

            return TestApplicationContext(
                hendelseKafkaConsumer = hendelseKafkaConsumer,
                azureConfig = azureConfig,
                applicationConfig = applicationConfig,
                serverConfig = serverConfig,
                securityConfig = securityConfig,
                prometheusMeterRegistry = prometheusMeterRegistry,
                kafkaKeysClient = kafkaKeysClient,
                oppslagApiClient = oppslagApiClient,
                hendelseRecordRepository = hendelseRecordRepository,
                brukerstoetteService = brukerstoetteService,
                additionalMeterBinder = additionalMeterBinder
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
        azureConfig = azureConfig,
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
