package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import no.nav.paw.arbeidssoekerregisteret.backup.health.isDatabaseReady
import no.nav.paw.arbeidssoekerregisteret.backup.health.isKafkaConsumerReady
import no.nav.paw.arbeidssoekerregisteret.backup.utils.configureTestClient
import no.nav.paw.startup.StartupCheck
import no.nav.paw.startup.startupPath
import no.nav.paw.startup.startupRoute

class StartupCheckTest : FreeSpec({
    with(TestApplicationContext.buildWithDatabase()) {
        "Alle startup checks er ok" {
            testApplication {
                every { hendelseConsumerWrapper.isRunning() } returns true
                configureInternalTestApplication(
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
                    { isKafkaConsumerReady(hendelseConsumerWrapper) },
                )
                val client = configureTestClient()
                val response = client.get(startupPath)
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
    }
})

fun ApplicationTestBuilder.configureInternalTestApplication(
    vararg startupChecks: StartupCheck,
) = application {
    routing {
        startupRoute(*startupChecks)
    }
}

