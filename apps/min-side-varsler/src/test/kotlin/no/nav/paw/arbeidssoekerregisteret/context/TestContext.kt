package no.nav.paw.arbeidssoekerregisteret.context

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jwt.SignedJWT
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.MIN_SIDE_VARSEL_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.model.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.repository.BestillingRepository
import no.nav.paw.arbeidssoekerregisteret.repository.BestiltVarselRepository
import no.nav.paw.arbeidssoekerregisteret.repository.EksterntVarselRepository
import no.nav.paw.arbeidssoekerregisteret.repository.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.repository.VarselRepository
import no.nav.paw.arbeidssoekerregisteret.route.bestillingerRoutes
import no.nav.paw.arbeidssoekerregisteret.route.varselRoutes
import no.nav.paw.arbeidssoekerregisteret.service.BestillingService
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderRequiredClaims
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.jackson.buildObjectMapper
import no.nav.paw.serialization.jackson.configureJackson
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration
import java.util.*
import javax.sql.DataSource

open class TestContext(
    open val logger: Logger = LoggerFactory.getLogger("test.logger"),
    val objectMapper: ObjectMapper = buildObjectMapper,
    open val mockOAuth2Server: MockOAuth2Server,
    open val dataSource: DataSource,
    open val resetDatabaseSql: List<String>,
    open val serverConfig: ServerConfig,
    open val applicationConfig: ApplicationConfig,
    open val securityConfig: SecurityConfig,
    open val prometheusMeterRegistry: PrometheusMeterRegistry,
    open val periodeRepository: PeriodeRepository,
    open val varselRepository: VarselRepository,
    open val eksternVarselRepository: EksterntVarselRepository,
    open val bestillingRepository: BestillingRepository,
    open val bestiltVarselRepository: BestiltVarselRepository,
    open val varselService: VarselService,
    open val bestillingService: BestillingService
) {
    fun initDatabase() {
        Database.connect(dataSource)
        transaction {
            resetDatabaseSql.forEach { exec(it) }
        }
        Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .load()
            .migrate()
    }

    private fun createApplicationContext() = ApplicationContext(
        serverConfig = serverConfig,
        applicationConfig = applicationConfig,
        securityConfig = securityConfig.copy(authProviders = mockOAuth2Server.createAuthProviders()),
        dataSource = dataSource,
        prometheusMeterRegistry = prometheusMeterRegistry,
        healthIndicatorRepository = HealthIndicatorRepository(),
        varselService = varselService,
        bestillingService = bestillingService,
        kafkaProducerList = listOf(),
        kafkaStreamsList = listOf(),
        kafkaShutdownTimeout = applicationConfig.kafkaShutdownTimeout
    )

    fun ApplicationTestBuilder.configureTestApplication() {
        with(createApplicationContext()) {
            application {
                installContentNegotiationPlugin()
                installErrorHandlingPlugin()
                installAuthenticationPlugin(securityConfig.authProviders)
                routing {
                    varselRoutes(varselService)
                    bestillingerRoutes(applicationConfig, bestillingService)
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

    fun MockOAuth2Server.issueAzureToken(
        oid: UUID = UUID.randomUUID(),
        name: String = "Kari Nordmann",
        navIdent: String = "NAV1234"
    ): SignedJWT {
        return issueToken(
            claims = mapOf(
                "oid" to oid.toString(),
                "name" to name,
                "NAVident" to navIdent
            )
        )
    }

    companion object {
        fun buildWithH2(): TestContext =
            build(buildH2DataSource(), listOf("DROP ALL OBJECTS"))

        fun buildWithPostgres(): TestContext =
            build(buildPostgresDataSource(), listOf("DROP SCHEMA public CASCADE", "CREATE SCHEMA public"))

        fun build(dataSource: DataSource, resetDatabaseSql: List<String>): TestContext {
            val mockOAuth2Server = MockOAuth2Server()
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig: ApplicationConfig =
                loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val periodeRepository = PeriodeRepository()
            val varselRepository = VarselRepository()
            val eksternVarselRepository = EksterntVarselRepository()
            val bestillingRepository = BestillingRepository()
            val bestiltVarselRepository = BestiltVarselRepository()
            val varselKafkaProducer = MockProducer(true, StringSerializer(), StringSerializer())
            val varselMeldingBygger = VarselMeldingBygger(
                runtimeEnvironment = serverConfig.runtimeEnvironment,
                minSideVarselConfig = loadNaisOrLocalConfiguration<MinSideVarselConfig>(MIN_SIDE_VARSEL_CONFIG)
            )
            val varselService = VarselService(
                applicationConfig = applicationConfig,
                meterRegistry = prometheusMeterRegistry,
                periodeRepository = periodeRepository,
                varselRepository = varselRepository,
                eksterntVarselRepository = eksternVarselRepository,
                varselMeldingBygger = varselMeldingBygger
            )
            val bestillingService = BestillingService(
                applicationConfig = applicationConfig,
                meterRegistry = prometheusMeterRegistry,
                bestillingRepository = bestillingRepository,
                bestiltVarselRepository = bestiltVarselRepository,
                varselRepository = varselRepository,
                varselKafkaProducer = varselKafkaProducer,
                varselMeldingBygger = varselMeldingBygger
            )

            return TestContext(
                mockOAuth2Server = mockOAuth2Server,
                dataSource = dataSource,
                resetDatabaseSql = resetDatabaseSql,
                serverConfig = serverConfig,
                applicationConfig = applicationConfig,
                securityConfig = securityConfig,
                prometheusMeterRegistry = prometheusMeterRegistry,
                periodeRepository = periodeRepository,
                varselRepository = varselRepository,
                eksternVarselRepository = eksternVarselRepository,
                bestillingRepository = bestillingRepository,
                bestiltVarselRepository = bestiltVarselRepository,
                varselService = varselService,
                bestillingService = bestillingService
            )
        }

        fun buildH2DataSource(): HikariDataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
                driverClassName = "org.h2.Driver"
                maximumPoolSize = 3
                isAutoCommit = true
                connectionTimeout = Duration.ofSeconds(5).toMillis()
                idleTimeout = Duration.ofMinutes(5).toMillis()
                maxLifetime = Duration.ofMinutes(10).toMillis()
            }
        )

        fun buildPostgresDataSource(): HikariDataSource {
            val databaseConfig = postgresContainer().let {
                DatabaseConfig(
                    host = it.host,
                    port = it.firstMappedPort,
                    database = it.databaseName,
                    username = it.username,
                    password = it.password
                )
            }
            return HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = databaseConfig.buildJdbcUrl()
                    driverClassName = "org.postgresql.Driver"
                    maximumPoolSize = 3
                    isAutoCommit = true
                    connectionTimeout = Duration.ofSeconds(5).toMillis()
                    idleTimeout = Duration.ofMinutes(5).toMillis()
                    maxLifetime = Duration.ofMinutes(10).toMillis()
                }
            )
        }

        private fun postgresContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
            val postgres = PostgreSQLContainer("postgres:17").apply {
                addEnv("POSTGRES_PASSWORD", "test")
                addEnv("POSTGRES_USER", "test")
                addEnv("POSTGRES_DB", "test")
                addExposedPorts(5433)
            }
            postgres.start()
            postgres.waitingFor(Wait.forHealthcheck())
            return postgres
        }

        private fun MockOAuth2Server.createAuthProviders(): List<AuthProvider> {
            val wellKnownUrl = wellKnownUrl("default").toString()
            return listOf(
                AuthProvider(
                    name = AzureAd.name,
                    audiences = listOf("default"),
                    discoveryUrl = wellKnownUrl,
                    requiredClaims = AuthProviderRequiredClaims(listOf("NAVident"))
                )
            )
        }
    }
}
