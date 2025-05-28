package no.nav.paw.arbeidssoekerregisteret.backup

import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.health.isDatabaseReady
import no.nav.paw.arbeidssoekerregisteret.backup.health.isKafkaConsumerReady
import no.nav.paw.arbeidssoekerregisteret.backup.health.startupPath
import no.nav.paw.arbeidssoekerregisteret.backup.health.startupRoute
import no.nav.paw.arbeidssoekerregisteret.backup.utils.configureTestClient
import org.apache.kafka.common.TopicPartition

class StartupProbeTest : FreeSpec({
    with(TestApplicationContext.buildWithDatabase()) {
        "Alle startup checks er ok" {
            testApplication {
                every { hendelseConsumer.assignment() } returns setOf(TopicPartition("topic", 1))
                configureInternalTestApplication(
                    applicationContext = toApplicationContext(),
                    startupChecks = listOf(
                        { isDatabaseReady(dataSource) },
                        { isKafkaConsumerReady(hendelseConsumer) },
                    )
                )
                val client = configureTestClient()
                val response = client.get(startupPath)
                response.status shouldBe HttpStatusCode.OK
            }
        }

        "En startup check feiler" {
            testApplication {
                val hikariDataSource = dataSource as HikariDataSource
                hikariDataSource.close()
                configureInternalTestApplication(
                    applicationContext = toApplicationContext(),
                    startupChecks = listOf(
                        { true }, { isDatabaseReady(dataSource) }
                    ),
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
    startupChecks: List<() -> Boolean> = listOf({ true }, { true }),
) =
    with(applicationContext) {
        application {
            routing {
                startupRoute(*startupChecks.toTypedArray())
            }
        }
    }

