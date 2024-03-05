package no.nav.paw.arbeidssokerregisteret

import io.kotest.core.spec.style.FunSpec
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.Json
import no.nav.paw.arbeidssokerregisteret.application.RequestHandler
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes

class ApplicationTest: FunSpec({
    test("Application Test") {
    testApplication {
        routing {
            val reqeustHandler: RequestHandler = mockk()
            arbeidssokerRoutes(reqeustHandler)
        }

            val response = client.put("/api/v1/arbeidssoker/periode") {
                setBody(
                    """ {
                "identitetsnummer": ${TestData.foedselsnummer.verdi},
                "periodeTilstand": "STARTET"
                }
                """)
                headers {
                    append(HttpHeaders.ContentType, "text/json")
                }
            }
        //assertTrue(response.headers[HttpHeaders.ContentType]?.contains(ContentType.Application.Json.toString()) ?: false)
        val isJSON = try {
            Json.parseToJsonElement(response.bodyAsText())
            true // Parsing successful, response body is JSON
        } catch (e: Exception) {
            false // Parsing failed, response body is not JSON
        }

        //assertTrue(isJSON)

        }
    }
})


