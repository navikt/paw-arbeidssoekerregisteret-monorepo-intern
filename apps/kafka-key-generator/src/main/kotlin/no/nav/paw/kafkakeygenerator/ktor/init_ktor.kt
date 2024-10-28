package no.nav.paw.kafkakeygenerator.ktor

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.paw.kafkakeygenerator.merge.MergeDetector

fun initKtorServer(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applikasjon: Applikasjon,
    mergeDetector: MergeDetector
) = embeddedServer(
    factory = Netty,
    port = 8080,
    configure = {
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
    }
) {
    konfigurerServer(
        autentiseringKonfigurasjon = autentiseringKonfigurasjon,
        prometheusMeterRegistry = prometheusMeterRegistry,
        applikasjon = applikasjon,
        mergeDetector = mergeDetector
    )
}
