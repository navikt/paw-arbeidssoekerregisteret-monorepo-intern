package no.nav.paw.security.test

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.paw.error.handler.handleException
import no.nav.paw.security.authorization.interceptor.authorize
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.policy.AccessPolicy
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
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

    fun Application.configureApplication(policies: List<AccessPolicy> = emptyList()) {
        configureSerialization()
        configureRequestHandling()
        configureAuthentication()
        configureRouting(policies)
    }

    fun Application.configureRouting(policies: List<AccessPolicy> = emptyList()) {
        routing {
            authenticate("tokenx") {
                get("/api/dummy") {
                    authorize(Action.READ, policies) {
                        call.respond(TestResponse("All Quiet on the Western Front"))
                    }
                }

                post("/api/dummy") {
                    authorize(Action.WRITE, policies) {
                        call.respond(TestResponse("All Quiet on the Western Front"))
                    }
                }
            }
        }
    }

    fun Application.configureSerialization() {
        install(ServerContentNegotiation) {
            jackson {
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                registerModule(JavaTimeModule())
                registerKotlinModule()
            }
        }
    }

    fun Application.configureRequestHandling() {
        install(IgnoreTrailingSlash)
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.handleException(cause)
            }
        }
    }


    fun Application.configureAuthentication() {
        val authProviders = mockOAuth2Server.getAuthProviders()

        authentication {
            authProviders.forEach { authProvider ->
                tokenValidationSupport(
                    name = authProvider.name,
                    config = TokenSupportConfig(
                        IssuerConfig(
                            name = authProvider.name,
                            discoveryUrl = authProvider.discoveryUrl,
                            acceptedAudience = listOf(authProvider.clientId)
                        )
                    ),
                    requiredClaims = RequiredClaims(
                        authProvider.name,
                        authProvider.claims.map.toTypedArray(),
                        authProvider.claims.combineWithOr
                    )
                )
            }
        }
    }
}

data class TestResponse(val message: String)
