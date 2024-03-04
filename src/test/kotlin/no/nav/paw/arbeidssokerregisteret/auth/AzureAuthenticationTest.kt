package no.nav.paw.arbeidssokerregisteret.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssokerregisteret.domain.navAnsatt
import no.nav.paw.arbeidssokerregisteret.requestScope
import no.nav.security.mock.oauth2.MockOAuth2Server

class AzureAuthenticationTest : FunSpec({
    val oauth = MockOAuth2Server()
    val testAuthUrl = "/testAuthTokenx"

    beforeSpec {
        oauth.start(8081)
    }

    afterSpec {
        oauth.shutdown()
    }
    test("For a authenticated Azure request we should be able to resolve 'NavAnsatt'") {
        testApplication {
            application { configureAuthentication(oauth) }
            val tokenMap = mapOf(
                "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
                "NAVident" to "test"
            )
            val token = oauth.issueToken(
                claims = tokenMap
            )
            routing {
                authenticate("azure") {
                    get("/test") {
                        val scope = requestScope()
                        scope.shouldNotBeNull()
                        val navAnsatt = navAnsatt(scope.claims)
                        navAnsatt.shouldNotBeNull()
                        navAnsatt.ident shouldBe tokenMap["NAVident"]
                        navAnsatt.azureId.toString() shouldBe tokenMap["oid"]
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
            val response = client.get("/test") { bearerAuth(token.serialize()) }
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
