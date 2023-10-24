package no.nav.paw.kafkakeygenerator.webserver

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

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
        tokenValidationSupport(
            name = autentiseringskonfigurasjon.name,
            config = TokenSupportConfig(
                IssuerConfig(
                    name = autentiseringskonfigurasjon.name,
                    discoveryUrl = autentiseringskonfigurasjon.discoveryUrl,
                    acceptedAudience = listOf(autentiseringskonfigurasjon.acceptedAudience),
                ),
            )
        )
    }
}