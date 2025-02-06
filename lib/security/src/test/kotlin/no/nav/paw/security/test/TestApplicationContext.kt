package no.nav.paw.security.test

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.paw.error.plugin.ErrorHandlingPlugin
import no.nav.paw.security.authentication.config.asRequiredClaims
import no.nav.paw.security.authentication.config.asTokenSupportConfig
import no.nav.paw.security.authentication.plugin.autentisering
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.IdPorten
import no.nav.paw.security.authentication.model.MaskinPorten
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.policy.AccessPolicy
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v3.tokenValidationSupport
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class TestApplicationContext {

    val mockOAuth2Server = MockOAuth2Server()

    fun ApplicationTestBuilder.configureTestClient(): HttpClient {
        return createClient {
            install(ClientContentNegotiation) {
                jackson {
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    registerModule(JavaTimeModule())
                    registerKotlinModule()
                }
            }
        }
    }

    fun Application.configureTestApplication(policies: List<AccessPolicy> = emptyList()) {
        configureSerialization()
        configureRequestHandling()
        configureAuthentication()
        configureRouting(policies)
    }

    private fun Application.configureRouting(policies: List<AccessPolicy> = emptyList()) {
        routing {
            route("/api/tokenx") {
                autentisering(TokenX) {
                    get("/") {
                        autorisering(Action.READ, policies) {
                            call.respond(TestResponse("All Quiet on the Western Front"))
                        }
                    }

                    post("/") {
                        autorisering(Action.WRITE, policies) {
                            call.respond(TestResponse("All Quiet on the Western Front"))
                        }
                    }
                }
            }

            route("/api/azuread") {
                autentisering(AzureAd) {
                    get("/") {
                        autorisering(Action.READ, policies) {
                            call.respond(TestResponse("All Quiet on the Western Front"))
                        }
                    }

                    post("/") {
                        autorisering(Action.WRITE, policies) {
                            call.respond(TestResponse("All Quiet on the Western Front"))
                        }
                    }
                }
            }

            route("/api/idporten") {
                autentisering(IdPorten) {
                    get("/") {
                        autorisering(Action.READ, policies) {
                            call.respond(TestResponse("All Quiet on the Western Front"))
                        }
                    }

                    post("/") {
                        autorisering(Action.WRITE, policies) {
                            call.respond(TestResponse("All Quiet on the Western Front"))
                        }
                    }
                }
            }

            route("/api/maskinporten") {
                autentisering(MaskinPorten) {
                    get("/") {
                        autorisering(Action.READ, policies) {
                            call.respond(TestResponse("All Quiet on the Western Front"))
                        }
                    }

                    post("/") {
                        autorisering(Action.WRITE, policies) {
                            call.respond(TestResponse("All Quiet on the Western Front"))
                        }
                    }
                }
            }
        }
    }

    private fun Application.configureSerialization() {
        install(ServerContentNegotiation) {
            jackson {
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                registerModule(JavaTimeModule())
                registerKotlinModule()
            }
        }
    }

    private fun Application.configureRequestHandling() {
        install(IgnoreTrailingSlash)
        install(ErrorHandlingPlugin)
    }


    private fun Application.configureAuthentication() {
        val authProviders = mockOAuth2Server.getAuthProviders()

        authentication {
            authProviders.forEach { provider ->
                tokenValidationSupport(
                    name = provider.name,
                    config = provider.asTokenSupportConfig(),
                    requiredClaims = provider.asRequiredClaims()
                )
            }
        }
    }
}

data class TestResponse(val message: String)
