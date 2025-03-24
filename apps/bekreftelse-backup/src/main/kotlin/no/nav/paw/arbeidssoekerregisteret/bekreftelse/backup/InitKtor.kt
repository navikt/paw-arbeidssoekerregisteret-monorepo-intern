package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.health.configureHealthRoutes
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.health.installMetrics

fun initKtor(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    binders: List<MeterBinder>,
) {
    embeddedServer(Netty, port = 8080) {
        configureHTTP(binders, prometheusMeterRegistry)
        routing {
            configureHealthRoutes(prometheusMeterRegistry)
        }
    }.start(wait = false)
}


fun Application.configureHTTP(
    binders: List<MeterBinder>,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    installMetrics(binders, prometheusMeterRegistry)
}