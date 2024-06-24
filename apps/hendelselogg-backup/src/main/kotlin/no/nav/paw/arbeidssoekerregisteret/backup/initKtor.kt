package no.nav.paw.arbeidssoekerregisteret.backup

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.DetaljerRequest
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Feil
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.health.configureHealthRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.health.installMetrics
import no.nav.paw.kafkakeygenerator.auth.NaisEnv
import no.nav.paw.kafkakeygenerator.auth.currentNaisEnv
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun initKtor(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    binders: List<MeterBinder>,
    azureConfig: AzureConfig,
    brukerstoetteService: BrukerstoetteService
) {
    embeddedServer(Netty, port = 8080) {
        configureHTTP(binders, prometheusMeterRegistry)
        configureAuthentication(azureConfig)
        routing {
            swaggerUI(path = "docs/brukerstoette", swaggerFile = "openapi/Brukerstoette.yaml")
            configureHealthRoutes(prometheusMeterRegistry)
            authenticate("azure") {
                configureBrukerstoetteRoutes(brukerstoetteService)
            }
        }
    }.start(wait = false)
}

fun Route.configureBrukerstoetteRoutes(brukerstoetteService: BrukerstoetteService) {
    post("/api/v1/arbeidssoeker/detaljer") {
        runCatching {
            if (currentNaisEnv == NaisEnv.ProdGCP) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable, Feil(
                        melding = "Tjenesten er ikke tilgjengelig i produksjon",
                        feilKode = "utilgjengelig"
                    )
                )
            } else {
                val request = call.receive<DetaljerRequest>()
                brukerstoetteService.hentDetaljer(request.identitetsnummer)
            }
        }.onSuccess { detaljer ->
            detaljer?.let { call.respond(it) } ?: call.respond(
                HttpStatusCode.NotFound, Feil(
                    melding = "Ingen hendelser for bruker",
                    feilKode = "ikke funnet"
                )
            )
        }.onFailure {
            call.respond(
                HttpStatusCode.InternalServerError, Feil(
                    melding = "Feil ved henting av detaljer",
                    feilKode = "intern feil"
                )
            )
        }
    }
}

fun Application.configureAuthentication(azureConfig: AzureConfig) {
    authentication {
        tokenValidationSupport(
            name = azureConfig.name,
            config = TokenSupportConfig(
                IssuerConfig(
                    name = azureConfig.name,
                    discoveryUrl = azureConfig.discoveryUrl,
                    acceptedAudience = listOf(azureConfig.clientId)
                )
            )
        )
    }
}

fun Application.configureHTTP(
    binders: List<MeterBinder>,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    installMetrics(binders, prometheusMeterRegistry)
    install(IgnoreTrailingSlash)
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerModule(JavaTimeModule())
            registerKotlinModule()
        }
    }
}
