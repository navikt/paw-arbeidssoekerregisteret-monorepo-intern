package no.nav.paw.arbeidssokerregisteret.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication

class SwaggerRoutesTest : FunSpec({
    context("health routes") {
        test("should respond with 200 OK") {
            testApplication {
                routing {
                    swaggerRoutes()
                }
                client.get("/docs/opplysninger").status shouldBe HttpStatusCode.OK
                client.get("/docs/startstopp").status shouldBe HttpStatusCode.OK
            }
        }
    }
})
