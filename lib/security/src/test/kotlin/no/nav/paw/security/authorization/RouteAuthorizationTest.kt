package no.nav.paw.security.authorization

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.paw.security.authorization.policy.TestDenyPolicy
import no.nav.paw.security.authorization.policy.TestPermitPolicy
import no.nav.paw.security.test.TestApplicationContext
import no.nav.paw.security.test.issueTokenXToken

class RouteAuthorizationTest : FreeSpec({

    with(TestApplicationContext()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
        }

        "Skal få 401 Unauthorized uten Bearer Token header" {
            testApplication {
                application {
                    configureApplication()
                }

                val testClient = configureTestClient()

                val response = testClient.get("/api/dummy")

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "Skal få 403 Forbidden ved en DENY policy" {
            testApplication {
                application {
                    configureApplication(
                        listOf(
                            TestPermitPolicy(),
                            TestDenyPolicy()
                        )
                    )
                }

                val testClient = configureTestClient()

                val response = testClient.get("/api/dummy") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken(pid = "01017012345"))
                }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }
    }
})