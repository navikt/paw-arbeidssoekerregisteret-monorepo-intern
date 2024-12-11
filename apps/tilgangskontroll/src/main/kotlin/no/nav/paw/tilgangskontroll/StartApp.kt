package no.nav.paw.tilgangskontroll

import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.tilgangskontroll.api.models.TilgangskontrollRequestV1
import no.nav.paw.tilgangskontroll.api.models.TilgangskontrollResponseV1
import no.nav.paw.tilgangskontroll.api.validering.valider
import no.nav.paw.tilgangskontroll.ktorserver.AuthProvider
import no.nav.paw.tilgangskontroll.ktorserver.authProvidersOf
import no.nav.paw.tilgangskontroll.ktorserver.configureAuthentication
import no.nav.paw.tilgangskontroll.poaotilgang.PoaoConfig
import no.nav.paw.tilgangskontroll.poaotilgang.initPoaobackend
import no.nav.security.token.support.v3.RequiredClaims
import no.nav.security.token.support.v3.tokenValidationSupport
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("tilgangskontroll")
fun main() {
    logger.info("Starter tilgangskontroll...")
    val azureM2MClientConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    val authProviders = authProvidersOf(AuthProvider.EntraId)
    val httpClient = createHttpClient()
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val poaoConfig = loadNaisOrLocalConfiguration<PoaoConfig>("poao_tilgang_cfg.toml")
    val service: TilgangsTjenesteForAnsatte = initPoaobackend(
        m2mTokenClient = createAzureAdM2MTokenClient(azureProviderConfig = azureM2MClientConfig),
        httpClient = httpClient,
        poaoConfig = poaoConfig
    )
    embeddedServer(Netty, port = 8080) {
        install(MicrometerMetrics) {
            registry = prometheusMeterRegistry
            meterBinders = listOf(
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                JvmInfoMetrics()
            )
        }
        configureAuthentication(authProviders)
        routing {
            get("/internal/isAlive") {
                call.respondText("ALIVE")
            }
            get("/internal/isReady") {
                call.respondText("READY")
            }
            get("/internal/metrics") {
                call.respondText(prometheusMeterRegistry.scrape())
            }
            authenticate(AuthProvider.EntraId.name) {
                post("/api/v1/tilgang") {
                    val request: TilgangskontrollRequestV1 = call.receive<TilgangskontrollRequestV1>()
                    val validertTilgangskontrollRequest = request.valider()
                    val harTilgang = service.harAnsattTilgangTilPerson(
                        navIdent = validertTilgangskontrollRequest.navAnsatt,
                        identitetsnummer = validertTilgangskontrollRequest.person,
                        tilgang = validertTilgangskontrollRequest.tilgang
                    )
                    call.respond(TilgangskontrollResponseV1(harTilgang = harTilgang))
                }
            }
        }
    }.start(wait = true)
    logger.info("Avslutter tilgangskontroll...")
}

