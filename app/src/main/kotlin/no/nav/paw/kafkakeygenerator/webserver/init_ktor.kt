package no.nav.paw.kafkakeygenerator.webserver

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon

fun initKtorServer(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applikasjon: Applikasjon
) =
    embeddedServer(Netty, port = 8080) {
        konfigurerServer(autentiseringKonfigurasjon, prometheusMeterRegistry, applikasjon)
    }

private fun Application.konfigurerServer(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applikasjon: Applikasjon
) {
    autentisering(autentiseringKonfigurasjon)
    micrometerMetrics(prometheusMeterRegistry)
    routing {
        konfigurereHelse(prometheusMeterRegistry)
        konfigurerApi(autentiseringKonfigurasjon, applikasjon)
    }
}

