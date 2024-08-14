package no.nav.paw.kafkakeygenerator.ktor

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon

fun initKtorServer(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applikasjon: Applikasjon
) = embeddedServer(
    factory = Netty,
    port = 8080,
    configure = {
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
    }
) {
    konfigurerServer(autentiseringKonfigurasjon, prometheusMeterRegistry, applikasjon)
}
