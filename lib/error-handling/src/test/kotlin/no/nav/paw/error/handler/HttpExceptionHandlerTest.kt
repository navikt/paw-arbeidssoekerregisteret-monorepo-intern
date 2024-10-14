package no.nav.paw.error.handler

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.paw.error.model.ProblemDetails
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.application.install as serverInstall
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class HttpExceptionHandlerTest : FreeSpec({
    "Skal håndtere exceptions og returnere ProblemDetails response" {
        testApplication {
            application {
                serverInstall(IgnoreTrailingSlash)
                serverInstall(StatusPages) {
                    exception<Throwable> { call: ApplicationCall, cause: Throwable ->
                        call.handleException(cause)
                    }
                }
                serverInstall(ServerContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                        kotlinModule()
                    }
                }
                routing {
                    get("/api/400") {
                        throw BadRequestException("It's bad")
                    }
                }
            }

            val client = createClient {
                install(ClientContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                        kotlinModule()
                    }
                }
            }

            val response400 = client.get("/api/400")
            val responseBody404 = response400.body<ProblemDetails>()
            response400.status shouldBe HttpStatusCode.BadRequest
            responseBody404.status shouldBe HttpStatusCode.BadRequest
            responseBody404.code shouldBe "PAW_KUNNE_IKKE_TOLKE_FORESPOERSEL"
            responseBody404.title shouldBe HttpStatusCode.BadRequest.description
        }
    }
})