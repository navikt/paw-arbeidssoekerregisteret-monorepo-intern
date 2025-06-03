package no.nav.paw.health.readiness

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

class ReadinessTest : FreeSpec({
    "Dersom ingen readiness checks er definert, så returnerer vi OK" {
        testApplication {
            testApplication()
            val client = createClient { }
            val response = client.get(readinessPath)
            response.status shouldBe OK
        }
    }

    "Alle readiness checks er ok" {
        testApplication {
            testApplication({ true }, { true })
            val client = createClient { }
            val response = client.get(readinessPath)
            response.status shouldBe OK
        }
    }
    "En readiness check er ikke ok" {
        testApplication {
            testApplication({ true }, { false })
            val client = createClient { }
            val response = client.get(readinessPath)
            response.status shouldBe ServiceUnavailable
        }
    }
})

fun ApplicationTestBuilder.testApplication(vararg readinessChecks: ReadinessCheck) = application {
    routing {
        readinessRoute(*readinessChecks)
    }
}
