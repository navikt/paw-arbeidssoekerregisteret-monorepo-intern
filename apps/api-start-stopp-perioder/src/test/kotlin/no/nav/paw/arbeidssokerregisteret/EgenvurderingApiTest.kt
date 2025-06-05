package no.nav.paw.arbeidssokerregisteret

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.egenvurderingPath
import no.nav.paw.arbeidssokerregisteret.routes.egenvurderingRoute
import java.util.*

class EgenvurderingApiTest : FreeSpec({
    "202 Accepted - POST" {
        testApplication {
            configureTestApplication()
            val client = createClient {}
            val response = client.post(egenvurderingPath) {
                contentType(Application.Json)
                setBody(egenvurderingJson)
            }
            response.status shouldBe HttpStatusCode.Accepted
            response.headers["x-trace-id"] shouldNotBe null
        }
    }

    "400 BadRequest - POST" {
        testApplication {
            configureTestApplication()
            val client = createClient {}
            val response = client.post(egenvurderingPath) {
                contentType(Application.Json)
                setBody("""{ugyldigJson}""")
            }
            response.status shouldBe HttpStatusCode.BadRequest
            response.headers["x-trace-id"] shouldNotBe null
        }
    }

})

//language=JSON
val egenvurderingJson = """
{
  "periodeId": "${UUID.randomUUID()}",
  "profileringId": "${UUID.randomUUID()}",
  "egenvurdering": "ANTATT_GODE_MULIGHETER"
}
"""

fun ApplicationTestBuilder.configureTestApplication() {
    application {
        configureSerialization()
        configureHTTP()
        install(OtelTraceIdPlugin)
    }
    routing {
        egenvurderingRoute()
    }
}

