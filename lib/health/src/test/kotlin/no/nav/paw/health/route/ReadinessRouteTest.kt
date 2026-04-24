package no.nav.paw.health.route

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.paw.health.model.ReadinessCheck

class ReadinessTest : FreeSpec({
    "Dersom ingen readiness checks er definert, så returnerer vi OK" {
        testApplication {
            readinessCheckTestApplication()
            val client = createClient { }
            val response = client.get(readinessPath)
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "Alle readiness checks er ok" {
        testApplication {
            readinessCheckTestApplication({ true }, { true })
            val client = createClient { }
            val response = client.get(readinessPath)
            response.status shouldBe HttpStatusCode.OK
        }
    }
    "En readiness check er ikke ok" {
        testApplication {
            readinessCheckTestApplication({ true }, { false })
            val client = createClient { }
            val response = client.get(readinessPath)
            response.status shouldBe HttpStatusCode.ServiceUnavailable
        }
    }
})

fun ApplicationTestBuilder.readinessCheckTestApplication(vararg readinessChecks: ReadinessCheck) = application {
    routing {
        readinessRoute(*readinessChecks)
    }
}