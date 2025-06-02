package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.health.StartupProbe
import no.nav.paw.arbeidssoekerregisteret.backup.health.isDatabaseReady
import no.nav.paw.arbeidssoekerregisteret.backup.health.isKafkaConsumerReady
import no.nav.paw.arbeidssoekerregisteret.backup.health.startupPath
import no.nav.paw.arbeidssoekerregisteret.backup.health.startupRoute
import no.nav.paw.arbeidssoekerregisteret.backup.utils.configureTestClient

class StartupProbeTest : FreeSpec({
    with(TestApplicationContext.buildWithDatabase()) {
        "Alle startup checks er ok" {
            testApplication {
                every { hendelseConsumerWrapper.isRunning() } returns true
                configureInternalTestApplication(
                    applicationContext = asApplicationContext(),
                    { isDatabaseReady(dataSource) },
                    { isKafkaConsumerReady(hendelseConsumerWrapper) },
                )
                val client = configureTestClient()
                val response = client.get(startupPath)
                response.status shouldBe HttpStatusCode.OK
            }
        }

        "Startup check feiler på database" {
            testApplication {
                dataSource.close()
                configureInternalTestApplication(
                    applicationContext = asApplicationContext(),
                    { isDatabaseReady(dataSource) },
                )
                val client = configureTestClient()
                val response = client.get(startupPath)
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }

        "Startup check feiler på kafka consumer" {
            testApplication {
                every { hendelseConsumerWrapper.isRunning() } returns false
                configureInternalTestApplication(
                    applicationContext = asApplicationContext(),
                    { isKafkaConsumerReady(hendelseConsumerWrapper) },
                )
                val client = configureTestClient()
                val response = client.get(startupPath)
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
        "Startup check feiler så lenge en av sjekken feiler" {
            testApplication {
                configureInternalTestApplication(
                    applicationContext = asApplicationContext(),
                    { true },
                    { false }
                )
                val client = configureTestClient()
                val response = client.get(startupPath)
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
    }
})

fun ApplicationTestBuilder.configureInternalTestApplication(
    applicationContext: ApplicationContext,
    vararg startupChecks: StartupProbe,
) = with(applicationContext) {
    application {
        routing {
            startupRoute(*startupChecks)
        }
    }
}

