package no.nav.paw.kafkakeygenerator.ktor

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.api.v1.konfigurerApi
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.konfigurerServer(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applikasjon: Applikasjon
) {
    autentisering(autentiseringKonfigurasjon)
    micrometerMetrics(prometheusMeterRegistry)
    serialisering()
    routing {
        konfigurereHelse(prometheusMeterRegistry)
        konfigurerApi(autentiseringKonfigurasjon, applikasjon)
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")
    }
}

fun Application.micrometerMetrics(prometheusMeterRegistry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
        )
    }
}

fun Application.autentisering(autentiseringskonfigurasjon: Autentiseringskonfigurasjon) {
    authentication {
        autentiseringskonfigurasjon.providers.forEach { provider ->
            tokenValidationSupport(
                name = provider.name,
                requiredClaims = RequiredClaims(
                    issuer = provider.name,
                    claimMap = provider.requiredClaims.toTypedArray()
                ),
                config = TokenSupportConfig(
                    IssuerConfig(
                        name = provider.name,
                        discoveryUrl = provider.discoveryUrl,
                        acceptedAudience = provider.acceptedAudience
                    ),
                ),
            )
        }
    }
}

fun Application.serialisering() {
    install(ContentNegotiation) {
        jackson()
    }
}