package no.nav.paw.arbeidssokerregisteret

import io.kotest.core.spec.style.FunSpec
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.util.reflect.*
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.application.RequestHandler
import no.nav.paw.arbeidssokerregisteret.application.invoke
import no.nav.paw.arbeidssokerregisteret.auth.configureAuthentication
import no.nav.paw.arbeidssokerregisteret.domain.http.PeriodeTilstand
import no.nav.paw.arbeidssokerregisteret.domain.http.StartStoppRequest
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes
import no.nav.security.mock.oauth2.MockOAuth2Server

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
            println(response)
        }
    }
})


