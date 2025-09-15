package no.nav.paw.kafkakeygenerator.context

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentitetHendelseDeserializer
import no.nav.paw.identitet.internehendelser.IdentitetHendelseSerializer
import no.nav.paw.kafkakeygenerator.config.APPLICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.config.SERVER_CONFIG
import no.nav.paw.kafkakeygenerator.config.ServerConfig
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import no.nav.paw.kafkakeygenerator.plugin.configureRouting
import no.nav.paw.kafkakeygenerator.repository.HendelseRepository
import no.nav.paw.kafkakeygenerator.repository.HwmRepository
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysIdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.repository.KonfliktIdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KonfliktRepository
import no.nav.paw.kafkakeygenerator.repository.PeriodeRepository
import no.nav.paw.kafkakeygenerator.service.HendelseService
import no.nav.paw.kafkakeygenerator.service.IdentitetService
import no.nav.paw.kafkakeygenerator.service.KafkaHwmOperations
import no.nav.paw.kafkakeygenerator.service.KafkaHwmService
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.service.KonfliktService
import no.nav.paw.kafkakeygenerator.service.PawHendelseKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PdlAktorKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PdlService
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.buildPostgresDataSource
import no.nav.paw.kafkakeygenerator.test.genererResponse
import no.nav.paw.kafkakeygenerator.test.runAsSql
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.CallId
import no.nav.paw.kafkakeygenerator.model.Either
import no.nav.paw.kafkakeygenerator.model.Failure
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import no.nav.paw.pdl.PdlClient
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderRequiredClaims
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.jackson.configureJackson
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.Producer
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import java.util.*
import javax.sql.DataSource

class TestContext private constructor(
    val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server(),
    val hendelseSerializer: IdentitetHendelseSerializer = IdentitetHendelseSerializer(),
    val hendelseDeserializer: IdentitetHendelseDeserializer = IdentitetHendelseDeserializer(),
    val serverConfig: ServerConfig = loadNaisOrLocalConfiguration(SERVER_CONFIG),
    val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG),
    val dataSource: DataSource,
    val initSql: List<String> = emptyList(),
    val pdlClient: PdlClient = pdlClient(),
    val meterRegistry: MeterRegistry = LoggingMeterRegistry(),
    val hwmRepository: HwmRepository = HwmRepository(),
    val kafkaKeysRepository: KafkaKeysRepository = KafkaKeysRepository(),
    val kafkaKeysAuditRepository: KafkaKeysAuditRepository = KafkaKeysAuditRepository(),
    val kafkaKeysIdentitetRepository: KafkaKeysIdentitetRepository = KafkaKeysIdentitetRepository(),
    val identitetRepository: IdentitetRepository = IdentitetRepository(),
    val periodeRepository: PeriodeRepository = PeriodeRepository(),
    val konfliktIdentitetRepository: KonfliktIdentitetRepository = KonfliktIdentitetRepository(),
    val konfliktRepository: KonfliktRepository = KonfliktRepository(konfliktIdentitetRepository),
    val hendelseRepository: HendelseRepository = HendelseRepository(),
    val pawIdentitetProducerMock: Producer<Long, IdentitetHendelse> = mockk<Producer<Long, IdentitetHendelse>>(),
    val pawHendelseloggProducerMock: Producer<Long, Hendelse> = mockk<Producer<Long, Hendelse>>(),
    val hendelseService: HendelseService = HendelseService(
        serverConfig = serverConfig,
        applicationConfig = applicationConfig,
        hendelseRepository = hendelseRepository,
        pawIdentitetHendelseProducer = pawIdentitetProducerMock,
        pawHendelseloggHendelseProducer = pawHendelseloggProducerMock
    ),
    val konfliktService: KonfliktService = KonfliktService(
        applicationConfig = applicationConfig,
        identitetRepository = identitetRepository,
        konfliktRepository = konfliktRepository,
        konfliktIdentitetRepository = konfliktIdentitetRepository,
        periodeRepository = periodeRepository,
        kafkaKeysIdentitetRepository = kafkaKeysIdentitetRepository,
        hendelseService = hendelseService
    ),
    val identitetService: IdentitetService = IdentitetService(
        identitetRepository = identitetRepository,
        konfliktService = konfliktService,
        hendelseService = hendelseService,
        kafkaKeysIdentitetRepository = kafkaKeysIdentitetRepository
    ),
    val pdlService: PdlService = PdlService(pdlClient),
    val kafkaKeysService: KafkaKeysService = KafkaKeysService(
        meterRegistry = meterRegistry,
        kafkaKeysRepository = kafkaKeysRepository,
        pdlService = pdlService,
        identitetService = identitetService
    ),
    val pdlAktorKafkaHwmOperations: KafkaHwmOperations = KafkaHwmService(
        kafkaConsumerConfig = applicationConfig.pdlAktorConsumer,
        meterRegistry = meterRegistry,
        hwmRepository = hwmRepository
    ),
    val pdlAktorKafkaConsumerService: PdlAktorKafkaConsumerService = PdlAktorKafkaConsumerService(
        kafkaConsumerConfig = applicationConfig.pdlAktorConsumer,
        hwmOperations = pdlAktorKafkaHwmOperations,
        identitetService = identitetService
    ),
    val pawHendelseKafkaConsumerService: PawHendelseKafkaConsumerService = PawHendelseKafkaConsumerService(
        meterRegistry = meterRegistry,
        kafkaKeysIdentitetRepository = kafkaKeysIdentitetRepository,
        kafkaKeysRepository = kafkaKeysRepository,
        kafkaKeysAuditRepository = kafkaKeysAuditRepository
    )
) {
    fun hentEllerOpprett(identitetsnummer: String): Either<Failure, ArbeidssoekerId> = runBlocking {
        kafkaKeysService.hentEllerOpprett(
            callId = CallId(UUID.randomUUID().toString()),
            identitet = Identitetsnummer(identitetsnummer)
        )
    }

    fun setUp(): TestContext {
        Database.connect(dataSource)
        initSql.forEach { it.runAsSql() }
        Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .load()
            .migrate()
        return this
    }

    fun tearDown(): TestContext {
        dataSource.connection.close()
        return this
    }

    private fun MockOAuth2Server.authProviders(): List<AuthProvider> {
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

    fun ApplicationTestBuilder.configureWebApplication() {
        application {
            installContentNegotiationPlugin()
            installErrorHandlingPlugin()
            installAuthenticationPlugin(mockOAuth2Server.authProviders())
            configureRouting(
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                healthIndicatorRepository = HealthIndicatorRepository(),
                kafkaKeysService = kafkaKeysService,
                mergeDetector = MergeDetector(pdlService, kafkaKeysRepository)
            )
        }
    }

    fun ApplicationTestBuilder.buildTestClient(): HttpClient {
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
        name: String = TestData.navName1,
        navIdent: String = TestData.navIdent1
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
        fun buildWithPostgres(): TestContext = TestContext(
            dataSource = buildPostgresDataSource(),
            initSql = listOf("DROP SCHEMA public CASCADE", "CREATE SCHEMA public")
        )

        private fun pdlClient(): PdlClient = PdlClient(
            url = "http://mock",
            tema = "tema",
            HttpClient(MockEngine {
                genererResponse(it)
            })
        ) { "fake token" }

        inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            setBody(body)
        }
    }
}
