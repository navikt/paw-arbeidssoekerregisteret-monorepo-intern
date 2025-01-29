package no.nav.paw.tilgangskontroll

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.tilgangskontroll.ktorserver.AuthProvider
import no.nav.paw.tilgangskontroll.ktorserver.AuthProviders
import no.nav.paw.tilgangskontroll.ktorserver.configureAuthentication
import no.nav.paw.tilgangskontroll.ktorserver.installContentNegotiation
import no.nav.paw.tilgangskontroll.ktorserver.installErrorHandling
import no.nav.paw.tilgangskontroll.routes.apiV1Tilgang
import java.time.Duration

fun initKtor(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    authProviders: AuthProviders,
    tilgangsTjenesteForAnsatte: TilgangsTjenesteForAnsatte
) = embeddedServer(Netty, port = 8080) {
    installMetrics(prometheusMeterRegistry)
    installContentNegotiation()
    installErrorHandling()
    configureAuthentication(authProviders)
    routing {
        healthAndMetricEndpoints(prometheusMeterRegistry)
        authenticate(AuthProvider.EntraId.name) {
            apiV1Tilgang(tilgangsTjenesteForAnsatte)
        }
    }
}

fun Application.installMetrics(prometheusMeterRegistry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            JvmInfoMetrics()
        )
        distributionStatisticConfig =
            DistributionStatisticConfig.builder()
                .percentilesHistogram(true)
                .maximumExpectedValue(Duration.ofSeconds(1).toNanos().toDouble())
                .minimumExpectedValue(Duration.ofMillis(20).toNanos().toDouble())
                .serviceLevelObjectives(
                    Duration.ofMillis(100).toNanos().toDouble(),
                    Duration.ofMillis(200).toNanos().toDouble()
                )
                .build()
    }
}

fun Routing.healthAndMetricEndpoints(prometheusMeterRegistry: PrometheusMeterRegistry) {
    get("/internal/isAlive") {
        call.respondText("ALIVE")
    }
    get("/internal/isReady") {
        call.respondText("READY")
    }
    get("/internal/metrics") {
        call.respondText(prometheusMeterRegistry.scrape())
    }
}
