package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.utils.configureTestClient
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.route.healthRoutes
import java.sql.SQLException
import javax.sql.DataSource

const val internalStartupUrl = "/internal/startup"

class StartupProbeTest : FreeSpec({
    val testApplicationContext = TestApplicationContext.buildWithDatabase()
    with(testApplicationContext) {
        beforeSpec { healthIndicatorRepository.getStartupIndicators().clear() }

        "Startup probe - alt er fint og flott" {
            testApplication {
                healthIndicatorRepository.startupIndicator(HealthStatus.HEALTHY)
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
    }

})

fun ApplicationTestBuilder.configureInternalTestApplication(applicationContext: ApplicationContext) {
    application {
        routing {
            healthRoutes(applicationContext.healthIndicatorRepository)
        }
    }
}

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
