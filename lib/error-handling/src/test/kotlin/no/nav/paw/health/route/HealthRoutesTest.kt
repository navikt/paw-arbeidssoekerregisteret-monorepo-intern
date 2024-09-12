package no.nav.paw.health.route

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.application.install as serverInstall
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class HealthRoutesTest : FreeSpec({

    "Endepunkter for helsesjekk skal returnere korrekt helsesjekk-status" {

        val healthIndicatorRepository = HealthIndicatorRepository()

        testApplication {
            application {
                serverInstall(IgnoreTrailingSlash)
                serverInstall(StatusPages)
                serverInstall(ServerContentNegotiation) {
                    jackson {}
                }
                routing {
                    healthRoutes(healthIndicatorRepository)
                }
            }

            val client = createClient {
                install(ClientContentNegotiation) {
                    jackson {}
                }
            }

            val livenessResponse1 = client.get("/internal/isAlive")
            val readinessResponse1 = client.get("/internal/isReady")

            livenessResponse1.status shouldBe HttpStatusCode.ServiceUnavailable
            livenessResponse1.body<String>() shouldBe HealthStatus.UNKNOWN.value
            readinessResponse1.status shouldBe HttpStatusCode.ServiceUnavailable
            readinessResponse1.body<String>() shouldBe HealthStatus.UNKNOWN.value

            val liveness1 = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())
            val liveness2 = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())
            val liveness3 = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())

            val readiness1 = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())
            val readiness2 = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())
            val readiness3 = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())

            val livenessResponse2 = client.get("/internal/isAlive")
            val readinessResponse2 = client.get("/internal/isReady")

            livenessResponse2.status shouldBe HttpStatusCode.OK
            livenessResponse2.body<String>() shouldBe HealthStatus.HEALTHY.value
            readinessResponse2.status shouldBe HttpStatusCode.ServiceUnavailable
            readinessResponse2.body<String>() shouldBe HealthStatus.UNKNOWN.value

            readiness1.setUnhealthy()
            liveness1.setUnhealthy()

            val livenessResponse3 = client.get("/internal/isAlive")
            val readinessResponse3 = client.get("/internal/isReady")

            livenessResponse3.status shouldBe HttpStatusCode.ServiceUnavailable
            livenessResponse3.body<String>() shouldBe HealthStatus.UNHEALTHY.value
            readinessResponse3.status shouldBe HttpStatusCode.ServiceUnavailable
            readinessResponse3.body<String>() shouldBe HealthStatus.UNHEALTHY.value

            readiness2.setUnhealthy()
            readiness3.setUnhealthy()
            liveness2.setUnhealthy()
            liveness3.setUnhealthy()

            val livenessResponse4 = client.get("/internal/isAlive")
            val readinessResponse4 = client.get("/internal/isReady")

            livenessResponse4.status shouldBe HttpStatusCode.ServiceUnavailable
            livenessResponse4.body<String>() shouldBe HealthStatus.UNHEALTHY.value
            readinessResponse4.status shouldBe HttpStatusCode.ServiceUnavailable
            readinessResponse4.body<String>() shouldBe HealthStatus.UNHEALTHY.value

            readiness1.setHealthy()
            liveness1.setHealthy()

            val livenessResponse5 = client.get("/internal/isAlive")
            val readinessResponse5 = client.get("/internal/isReady")

            livenessResponse5.status shouldBe HttpStatusCode.ServiceUnavailable
            livenessResponse5.body<String>() shouldBe HealthStatus.UNHEALTHY.value
            readinessResponse5.status shouldBe HttpStatusCode.ServiceUnavailable
            readinessResponse5.body<String>() shouldBe HealthStatus.UNHEALTHY.value

            readiness2.setHealthy()
            liveness2.setHealthy()

            val livenessResponse6 = client.get("/internal/isAlive")
            val readinessResponse6 = client.get("/internal/isReady")

            livenessResponse6.status shouldBe HttpStatusCode.ServiceUnavailable
            livenessResponse6.body<String>() shouldBe HealthStatus.UNHEALTHY.value
            readinessResponse6.status shouldBe HttpStatusCode.ServiceUnavailable
            readinessResponse6.body<String>() shouldBe HealthStatus.UNHEALTHY.value

            readiness3.setHealthy()
            liveness3.setHealthy()

            val livenessResponse7 = client.get("/internal/isAlive")
            val readinessResponse7 = client.get("/internal/isReady")

            livenessResponse7.status shouldBe HttpStatusCode.OK
            livenessResponse7.body<String>() shouldBe HealthStatus.HEALTHY.value
            readinessResponse7.status shouldBe HttpStatusCode.OK
            readinessResponse7.body<String>() shouldBe HealthStatus.HEALTHY.value
        }
    }
})