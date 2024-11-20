package no.nav.paw.kafkakeygenerator.ktor

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.config.AuthenticationConfig
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import no.nav.paw.kafkakeygenerator.plugin.configSerialization
import no.nav.paw.kafkakeygenerator.plugin.configureAuthentication
import no.nav.paw.kafkakeygenerator.plugin.configureErrorHandling
import no.nav.paw.kafkakeygenerator.plugin.configureLogging
import no.nav.paw.kafkakeygenerator.plugin.configureMetrics
import no.nav.paw.kafkakeygenerator.plugin.configureRouting

fun initKtorServer(
    autentiseringKonfigurasjon: AuthenticationConfig,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
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
    configSerialization()
    configureLogging()
    configureErrorHandling()
    configureAuthentication(autentiseringKonfigurasjon)
    configureMetrics(prometheusMeterRegistry)
    configureRouting(
        autentiseringKonfigurasjon,
        prometheusMeterRegistry,
        healthIndicatorRepository,
        applikasjon,
        mergeDetector
    )
}
