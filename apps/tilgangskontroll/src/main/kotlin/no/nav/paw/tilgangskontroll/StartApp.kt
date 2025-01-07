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
import org.slf4j.MarkerFactory

private val logger = LoggerFactory.getLogger("tilgangskontroll")

val secureLogger = LoggerFactory.getLogger("tjenestekall")
val secureMarker = MarkerFactory.getMarker("SECURE_LOG")

fun main() {
    secureLogger.info(secureMarker, "Starter tilgangskontroll med sikker logging...")
    logger.info("Starter tilgangskontroll...")
    val azureM2MClientConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    val authProviders = authProvidersOf(AuthProvider.EntraId)
    val httpClient = createHttpClient()
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val poaoConfig = loadPoaoConfig()
    val service: TilgangsTjenesteForAnsatte = initPoaobackend(
        m2mTokenClient = createAzureAdM2MTokenClient(azureProviderConfig = azureM2MClientConfig),
        httpClient = httpClient,
        poaoConfig = poaoConfig
    ).withSecureLogging(
        secureLogger = secureLogger,
        secureMarker = secureMarker
    )
    initKtor(
        prometheusMeterRegistry = prometheusMeterRegistry,
        authProviders = authProviders,
        tilgangsTjenesteForAnsatte = service
    ).start(wait = true)
    logger.info("Avslutter tilgangskontroll...")
}

