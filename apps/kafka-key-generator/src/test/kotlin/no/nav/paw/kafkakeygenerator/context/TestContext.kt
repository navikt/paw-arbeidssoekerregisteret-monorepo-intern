package no.nav.paw.kafkakeygenerator.context

import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.kafkakeygenerator.config.APPLICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.config.SERVER_CONFIG
import no.nav.paw.kafkakeygenerator.config.ServerConfig
import no.nav.paw.kafkakeygenerator.model.dto.CallId
import no.nav.paw.kafkakeygenerator.model.dto.Identitetsnummer
import no.nav.paw.kafkakeygenerator.plugin.configureRouting
import no.nav.paw.kafkakeygenerator.service.HendelseService
import no.nav.paw.kafkakeygenerator.service.IdentitetResponseService
import no.nav.paw.kafkakeygenerator.service.IdentitetService
import no.nav.paw.kafkakeygenerator.service.KafkaHwmOperations
import no.nav.paw.kafkakeygenerator.service.KafkaHwmService
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.service.KonfliktService
import no.nav.paw.kafkakeygenerator.service.PdlAktorKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PdlService
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestData.asGraphQLResponse
import no.nav.paw.kafkakeygenerator.test.TestData.asString
import no.nav.paw.kafkakeygenerator.test.buildPostgresDataSource
import no.nav.paw.kafkakeygenerator.test.runAsSql
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.graphql.generated.HentIdenter
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderRequiredClaims
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.jackson.buildObjectMapper
import no.nav.paw.serialization.jackson.configureJackson
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.Producer
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import java.util.*
import javax.sql.DataSource

private val objectMapper = buildObjectMapper

class TestContext private constructor(
    val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server(),
    val serverConfig: ServerConfig = loadNaisOrLocalConfiguration(SERVER_CONFIG),
    val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG),
    val dataSource: DataSource,
    val initSql: List<String> = emptyList(),
    val pdlClientMock: PdlClient = pdlClientMock(TestData::asPdlAktor),
    val meterRegistry: MeterRegistry = LoggingMeterRegistry(),
    val pawIdentitetProducerMock: Producer<Long, IdentitetHendelse> = mockk<Producer<Long, IdentitetHendelse>>(),
    val pawHendelseloggProducerMock: Producer<Long, Hendelse> = mockk<Producer<Long, Hendelse>>(),
    val hendelseService: HendelseService = HendelseService(
        serverConfig = serverConfig,
        applicationConfig = applicationConfig,
        pawIdentitetHendelseProducer = pawIdentitetProducerMock,
        pawHendelseloggHendelseProducer = pawHendelseloggProducerMock
    ),
    val konfliktService: KonfliktService = KonfliktService(
        applicationConfig = applicationConfig,
        hendelseService = hendelseService
    ),
    val identitetService: IdentitetService = IdentitetService(
        konfliktService = konfliktService,
        hendelseService = hendelseService,
    ),
    val identitetServiceMock: IdentitetService = mockk<IdentitetService>(),
    val pdlService: PdlService = PdlService(pdlClientMock),
    val identitetResponseService: IdentitetResponseService = IdentitetResponseService(
        pdlService = pdlService
    ),
    val kafkaKeysService: KafkaKeysService = KafkaKeysService(
        meterRegistry = meterRegistry,
        pdlService = pdlService,
        identitetService = identitetService
    ),
    val pdlAktorKafkaHwmOperations: KafkaHwmOperations = KafkaHwmService(
        kafkaConsumerConfig = applicationConfig.pdlAktorConsumer,
        meterRegistry = meterRegistry
    ),
    val pdlAktorKafkaConsumerService: PdlAktorKafkaConsumerService = PdlAktorKafkaConsumerService(
        kafkaConsumerConfig = applicationConfig.pdlAktorConsumer,
        hwmOperations = pdlAktorKafkaHwmOperations,
        identitetService = identitetServiceMock
    )
) {
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
                identitetResponseService = identitetResponseService
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

        private fun pdlClientMock(
            byggResponse: HentIdenter.() -> Aktor
        ): PdlClient = PdlClient(
            url = "http://mock",
            tema = "tema",
            httpClient = HttpClient(MockEngine { requestData ->
                genererResponse(
                    requestData = requestData,
                    byggResponse = byggResponse
                )
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

fun MockRequestHandleScope.genererResponse(
    requestData: HttpRequestData,
    byggResponse: (request: HentIdenter) -> Aktor
): HttpResponseData {
    val body = (requestData.body as TextContent).text
    val request = objectMapper.readValue<HentIdenter>(body)
    val aktor = byggResponse(request)
    return respond(
        content = aktor.asGraphQLResponse().asString(),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )
}

fun KafkaKeysService.hentEllerOpprett(identitet: String): Long = hentEllerOpprett(
    callId = CallId(UUID.randomUUID().toString()),
    identitet = Identitetsnummer(identitet)
).value

fun KafkaKeysService.hentEllerOppdater(identitet: String): Long = hentEllerOppdater(
    callId = CallId(UUID.randomUUID().toString()),
    identitet = Identitetsnummer(identitet)
).value
