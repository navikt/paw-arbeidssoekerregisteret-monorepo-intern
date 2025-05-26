package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.HwmRebalanceListener
import no.nav.paw.arbeidssoekerregisteret.backup.metrics.Metrics
import no.nav.paw.arbeidssoekerregisteret.backup.utils.configureTestClient
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.kafka.consumer.KafkaConsumerWrapper
import no.nav.paw.logging.logger.buildErrorLogger
import no.nav.paw.security.authentication.config.SecurityConfig
import java.sql.SQLException
import javax.sql.DataSource

const val internalStartupUrl = "/internal/startup"
val testLogger = buildErrorLogger

class StartupProbeTest : FreeSpec({

    "Startup probe - alt er fint og flott" {
        val testApplicationContext = TestApplicationContext.buildWithDatabase()
        testApplication {
            configureInternalTestApplication(testApplicationContext.toApplicationContext())
            val client = configureTestClient()
            val response = client.get(internalStartupUrl)
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "Startup probe - databasen er ikke tilkoblet" {
        val dataSourceMock = mockk<DataSource>().also {
            every { it.connection } throws SQLException("Database connection failed")
        }
        val applicationContext = testContextWith(dataSourceMock)
        testApplication {
            configureInternalTestApplication(applicationContext)
            val client = configureTestClient()
            val response = client.get(internalStartupUrl)
            response.status shouldBe HttpStatusCode.InternalServerError
        }
    }
    "Startup probe - kafka feil" {
        val applicationContext = TestApplicationContext.buildWithDatabase()
        testApplication {
            configureInternalTestApplication(applicationContext.toApplicationContext())
            val client = configureTestClient()
            val response = client.get(internalStartupUrl)
            response.status shouldBe HttpStatusCode.InternalServerError
        }
    }
})

fun ApplicationTestBuilder.configureInternalTestApplication(applicationContext: ApplicationContext) {
    with(applicationContext) {
        application {
            routing {
                get(internalStartupUrl) {
                    val kafkaOk = hendelseConsumerWrapper.isRunning()
                    val canConnectToDb = isConnected(dataSource)
                    if (!canConnectToDb) {
                        call.respondText(
                            "Application is not ready to receive requests, database connection failed",
                            status = HttpStatusCode.InternalServerError,
                            contentType = ContentType.Text.Plain
                        )
                    } else if(!kafkaOk) {
                        call.respondText(
                            "Application is not ready to receive requests, kafka consumer connection failed",
                            status = HttpStatusCode.InternalServerError,
                            contentType = ContentType.Text.Plain
                        )
                    } else {
                        call.respondText(
                            "Application is started and ready to receive requests",
                            status = HttpStatusCode.OK,
                            contentType = ContentType.Text.Plain
                        )
                    }
                }
            }
        }
    }
}

fun isConnected(dataSource: DataSource) = runCatching {
    dataSource.connection.use { conn ->
        conn.prepareStatement("SELECT 1").execute()
    }
}.onFailure { error ->
    testLogger.error("Db connection error", error)
}.onSuccess {
    testLogger.info("Db connection successful")
}.isSuccess

private fun testContextWith(dataSourceMock: DataSource): ApplicationContext = ApplicationContext(
    applicationConfig = mockk(relaxed = true),
    serverConfig = mockk(relaxed = true),
    securityConfig = mockk(relaxed = true),
    dataSource = dataSourceMock,
    prometheusMeterRegistry = mockk(relaxed = true),
    hwmRebalanceListener = mockk(relaxed = true),
    hendelseConsumerWrapper = mockk(relaxed = true),
    brukerstoetteService = mockk(relaxed = true),
    additionalMeterBinder = mockk(relaxed = true),
    metrics = mockk(relaxed = true),
    backupService = mockk(relaxed = true),
)
