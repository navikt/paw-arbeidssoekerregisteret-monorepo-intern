package no.nav.paw.kafkakeygenerator

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import no.nav.paw.kafkakeygenerator.test.genererResponse
import no.nav.paw.kafkakeygenerator.test.initTestDatabase
import no.nav.paw.pdl.PdlClient
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderRequiredClaims
import no.nav.paw.security.authentication.config.SecurityConfig

fun main() {
    val dataSource = initTestDatabase()
    val pdlKlient = PdlClient(
        url = "http://mock",
        tema = "tema",
        HttpClient(MockEngine {
            genererResponse(it)
        })
    ) { "fake token" }
    startApplication(
        SecurityConfig(
            authProviders = listOf(
                AuthProvider(
                    name = "mock",
                    discoveryUrl = "http://localhost:8081/.well-known/openid-configuration",
                    audiences = listOf("mock"),
                    requiredClaims = AuthProviderRequiredClaims(claims = listOf("mock"))
                )
            )
        ), dataSource, pdlKlient
    )
}