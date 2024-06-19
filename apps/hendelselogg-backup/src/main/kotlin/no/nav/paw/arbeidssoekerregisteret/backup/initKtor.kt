package no.nav.paw.arbeidssoekerregisteret.backup

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.DetaljerRequest
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.getMockResponse
import no.nav.paw.arbeidssoekerregisteret.backup.health.configureHealthRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.health.installMetrics
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import org.apache.kafka.clients.consumer.Consumer

fun initKtor(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    consumer: Consumer<*, *>
) {
    val azureConfig = loadNaisOrLocalConfiguration<AzureConfig>("azure.toml")
    embeddedServer(Netty, port = 8080) {
        installMetrics(consumer, prometheusMeterRegistry)
        authentication {
            tokenValidationSupport(
                name = azureConfig.name,
                requiredClaims = RequiredClaims(azureConfig.claim, arrayOf(azureConfig.claim)),
                config = TokenSupportConfig(
                    IssuerConfig(
                        name = azureConfig.name,
                        discoveryUrl = azureConfig.discoveryUrl,
                        acceptedAudience = listOf(azureConfig.clientId)
                    )
                )
            )
        }
        routing {
            swaggerUI(path = "docs/brukerstoette", swaggerFile = "openapi/Brukerstoette.yaml")
            configureHealthRoutes(prometheusMeterRegistry)
            authenticate {
                post("/api/v1/arbeidssoeker/detaljer") {
                    val request = call.receive<DetaljerRequest>()
                    call.respond(getMockResponse())
                }
                get("/hello") {
                    call.respondText("Hello, world!")
                }
            }
        }
    }.start(wait = false)
}