package no.nav.paw.health.route

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.paw.health.model.LivenessCheck

class LivenessTest : FreeSpec({
    "Dersom ingen liveness checks er definert, så returnerer vi OK" {
        testApplication {
            livenessCheckTestApplication()
            val client = createClient { }
            val response = client.get(livenessPath)
            response.status shouldBe HttpStatusCode.OK
        }
    }

    "Alle liveness checks er ok" {
        testApplication {
            livenessCheckTestApplication({ true }, { true })
            val client = createClient { }
            val response = client.get(livenessPath)
            response.status shouldBe HttpStatusCode.OK
        }
    }
    "En liveness check er ikke ok" {
        testApplication {
            livenessCheckTestApplication({ true }, { false })
            val client = createClient { }
            val response = client.get(livenessPath)
            response.status shouldBe HttpStatusCode.ServiceUnavailable
        }
    }
})

fun ApplicationTestBuilder.livenessCheckTestApplication(vararg livenessChecks: LivenessCheck) = application {
    routing {
        livenessRoute(*livenessChecks)
    }
}