package no.nav.paw.arbeidssokerregisteret.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class HealthRoutesTest : FunSpec({
    context("health routes") {
        test("should respond with 200 OK") {
            testApplication {
                application {
                    installRouting()
                }

                val isAliveResponse = client.get("/internal/isAlive")
                isAliveResponse.status shouldBe HttpStatusCode.OK

                val isReadyResponse = client.get("/internal/isReady")
                isReadyResponse.status shouldBe HttpStatusCode.OK

                val metricsResponse = client.get("/internal/metrics")
                metricsResponse.status shouldBe HttpStatusCode.OK
            }
        }
    }
})

fun Application.installRouting() {
    routing {
        healthRoutes(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
    }
}
