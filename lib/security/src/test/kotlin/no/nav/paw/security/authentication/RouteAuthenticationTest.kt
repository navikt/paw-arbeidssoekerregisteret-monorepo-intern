package no.nav.paw.security.authentication

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.paw.security.authentication.model.NavIdentHeader
import no.nav.paw.security.test.TestApplicationContext
import no.nav.paw.security.test.issueAzureADToken
import no.nav.paw.security.test.issueAzureM2MToken
import no.nav.paw.security.test.issueIDPortenToken
import no.nav.paw.security.test.issueMaskinPortenToken
import no.nav.paw.security.test.issueTokenXToken

class RouteAuthenticationTest : FreeSpec({

    with(TestApplicationContext()) {

        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
        }

        "Skal få 403 Forbidden uten Bearer Token header mot TokenX endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/tokenx")
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden uten Bearer Token header mot AzureAD endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/azuread")
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden uten Bearer Token header mot IDPorten endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/idporten")
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden uten Bearer Token header mot MaskinPorten endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/maskinporten")
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 200 OK ved TokenX token" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/tokenx") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }

        "Skal få 403 Forbidden ved TokenX token mot AzureAD endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/azuread") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden ved TokenX token mot IDPorten endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/idporten") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden ved TokenX token mot MaskinPorten endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/maskinporten") {
                    bearerAuth(mockOAuth2Server.issueTokenXToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 200 OK ved AzureAD token" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/azuread") {
                    bearerAuth(mockOAuth2Server.issueAzureADToken())
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }

        "Skal få 200 OK ved AzureAD M2M token" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/azuread") {
                    bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }

        "Skal få 200 OK ved AzureAD M2M token med Nav-Ident header" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/azuread") {
                    bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    header(NavIdentHeader.name, "NAV1234")
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }

        "Skal få 403 Forbidden ved AzureAD token mot TokenX endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/tokenx") {
                    bearerAuth(mockOAuth2Server.issueAzureADToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden ved AzureAD token mot IDPorten endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/idporten") {
                    bearerAuth(mockOAuth2Server.issueAzureADToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden ved AzureAD token mot MaskinPorten endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/maskinporten") {
                    bearerAuth(mockOAuth2Server.issueAzureADToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 200 OK ved IDPorten token" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/idporten") {
                    bearerAuth(mockOAuth2Server.issueIDPortenToken())
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }

        "Skal få 403 Forbidden ved IDPorten token mot TokenX endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/tokenx") {
                    bearerAuth(mockOAuth2Server.issueIDPortenToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden ved IDPorten token mot AzureAD endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/azuread") {
                    bearerAuth(mockOAuth2Server.issueIDPortenToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden ved IDPorten token mot MaskinPorten endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/maskinporten") {
                    bearerAuth(mockOAuth2Server.issueIDPortenToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 200 OK ved MaskinPorten token" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/maskinporten") {
                    bearerAuth(mockOAuth2Server.issueMaskinPortenToken())
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }

        "Skal få 403 Forbidden ved MaskinPorten token mot TokenX endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/tokenx") {
                    bearerAuth(mockOAuth2Server.issueMaskinPortenToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden ved MaskinPorten token mot AzureAD endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/azuread") {
                    bearerAuth(mockOAuth2Server.issueMaskinPortenToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        "Skal få 403 Forbidden ved MaskinPorten token mot IDPorten endepunkt" {
            testApplication {
                application {
                    configureTestApplication()
                }

                val testClient = configureTestClient()
                val response = testClient.get("/api/idporten") {
                    bearerAuth(mockOAuth2Server.issueMaskinPortenToken())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }
    }
})