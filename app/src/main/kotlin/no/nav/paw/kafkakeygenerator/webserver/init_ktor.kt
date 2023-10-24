package no.nav.paw.kafkakeygenerator.webserver

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.Api
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon

fun initKtorServer(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    api: Api
) =
    embeddedServer(Netty, port = 8080) {
        konfigurerServer(autentiseringKonfigurasjon, prometheusMeterRegistry, api)
    }

private fun Application.konfigurerServer(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    api: Api
) {
    autentisering(autentiseringKonfigurasjon)
    micrometerMetrics(prometheusMeterRegistry)
    routing {
        konfigurereHelse(prometheusMeterRegistry)
        konfigurerApi(autentiseringKonfigurasjon, api)
    }
}

