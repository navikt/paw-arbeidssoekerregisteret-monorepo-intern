package no.nav.paw.health.liveness

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

class LivenessTest : FreeSpec({
    "Dersom ingen liveness checks er definert, så returnerer vi OK" {
        testApplication {
            testApplication()
            val client = createClient { }
            val response = client.get(livenessPath)
            response.status shouldBe OK
        }
    }

    "Alle liveness checks er ok" {
        testApplication {
            testApplication({ true }, { true })
            val client = createClient { }
            val response = client.get(livenessPath)
            response.status shouldBe OK
        }
    }
    "En liveness check er ikke ok" {
        testApplication {
            testApplication({ true }, { false })
            val client = createClient { }
            val response = client.get(livenessPath)
            response.status shouldBe ServiceUnavailable
        }
    }
})

fun ApplicationTestBuilder.testApplication(vararg livenessChecks: LivenessCheck) = application {
    routing {
        livenessRoute(*livenessChecks)
    }
}

