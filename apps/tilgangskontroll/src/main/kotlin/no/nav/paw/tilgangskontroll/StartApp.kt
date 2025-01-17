package no.nav.paw.tilgangskontroll

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.tilgangskontroll.ktorserver.AuthProvider
import no.nav.paw.tilgangskontroll.ktorserver.authProvidersOf
import no.nav.paw.tilgangskontroll.poaotilgang.initPoaobackend
import no.nav.paw.tilgangskontroll.poaotilgang.loadPoaoConfig
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("tilgangskontroll")

fun main() {
    logger.info("Starter tilgangskontroll...")
    val service: TilgangsTjenesteForAnsatte = initPoaobackend(
        secureLogger = SecureLogger,
        m2mTokenClient = createAzureAdM2MTokenClient(azureProviderConfig = loadNaisOrLocalConfiguration(AZURE_M2M_CONFIG)),
        httpClient = createHttpClient(),
        poaoConfig = loadPoaoConfig()
    ).withSecureLogging(
        secureLogger = SecureLogger
    )
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    initKtor(
        prometheusMeterRegistry = prometheusMeterRegistry,
        authProviders = authProvidersOf(AuthProvider.EntraId),
        tilgangsTjenesteForAnsatte = service
    ).start(wait = true)
    logger.info("Avslutter tilgangskontroll...")
}

