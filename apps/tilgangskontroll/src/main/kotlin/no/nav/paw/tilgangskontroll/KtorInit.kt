package no.nav.paw.tilgangskontroll

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
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.tilgangskontroll.ktorserver.AuthProvider
import no.nav.paw.tilgangskontroll.ktorserver.AuthProviders
import no.nav.paw.tilgangskontroll.ktorserver.configureAuthentication
import no.nav.paw.tilgangskontroll.ktorserver.installContentNegotiation
import no.nav.paw.tilgangskontroll.ktorserver.installStatusPage
import no.nav.paw.tilgangskontroll.routes.apiV1Tilgang

fun initKtor(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    authProviders: AuthProviders,
    tilgangsTjenesteForAnsatte: TilgangsTjenesteForAnsatte
) = embeddedServer(Netty, port = 8080) {
    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            JvmInfoMetrics()
        )
    }
    installContentNegotiation()
    installStatusPage()
    configureAuthentication(authProviders)
    routing {
        healthAndMetricEndpoints(prometheusMeterRegistry)
        authenticate(AuthProvider.EntraId.name) {
            apiV1Tilgang(tilgangsTjenesteForAnsatte)
        }
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
