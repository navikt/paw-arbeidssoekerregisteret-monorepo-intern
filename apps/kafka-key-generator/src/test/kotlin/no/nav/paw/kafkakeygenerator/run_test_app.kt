package no.nav.paw.kafkakeygenerator

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import no.nav.paw.kafkakeygenerator.config.AuthenticationProviderConfig
import no.nav.paw.kafkakeygenerator.config.AuthenticationConfig
import no.nav.paw.pdl.PdlClient

fun main() {
    val dataSource = initTestDatabase()
    val pdlKlient = PdlClient(
        url = "http://mock",
        tema = "tema",
        HttpClient(MockEngine {
            genererResponse(it)
        })
    ) { "fake token" }
    startApplikasjon(AuthenticationConfig(
        providers = listOf(AuthenticationProviderConfig(
            name = "mock",
            discoveryUrl = "http://localhost:8081/.well-known/openid-configuration",
            acceptedAudience = listOf("mock"),
            cookieName = "mock",
            requiredClaims = listOf("mock")
        )),
        kafkaKeyApiAuthProvider = "mock"
    ), dataSource, pdlKlient)
}