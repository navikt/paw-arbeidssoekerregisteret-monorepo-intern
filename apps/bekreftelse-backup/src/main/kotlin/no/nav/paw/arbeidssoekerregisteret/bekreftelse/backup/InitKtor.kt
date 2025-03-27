package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.api.brukerstoette.models.Feil
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.api.brukerstoette.models.HendelserRequest
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.health.configureHealthRoutes
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.health.installMetrics
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.logging.logger.buildAuditLogger
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authentication.plugin.autentisering
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

private val errorLogger = LoggerFactory.getLogger("error_logger")

fun initKtor(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    binders: List<MeterBinder>,
    securityConfig: SecurityConfig,
    brukerstoetteService: BrukerstoetteService
) {
    embeddedServer(Netty, port = 8080) {
        configureHTTP(binders, prometheusMeterRegistry, securityConfig)
        install(CallLogging) {
            level = Level.INFO
            filter { call -> call.request.path().startsWith("/api") }
        }
        routing {
            swaggerUI(path = "docs/brukerstoette", swaggerFile = "openapi/Brukerstoette.yaml")
            configureHealthRoutes(prometheusMeterRegistry)
            if (currentRuntimeEnvironment is ProdGcp) {
                autentisering(AzureAd) {
                    configureBrukerstoetteRoutes(brukerstoetteService)
                }
            } else {
                configureBrukerstoetteRoutes(brukerstoetteService)
            }
        }
    }.start(wait = false)
}

private val auditLogger = buildAuditLogger
fun Route.configureBrukerstoetteRoutes(brukerstoetteService: BrukerstoetteService) {
    post("/api/v1/arbeidssoeker/bekreftelse-hendelser") {
        runCatching {
            val bruker = call.bruker<NavAnsatt>()
            val request: HendelserRequest = call.receive()
            auditLogger.audit("Brukerstoette-bekreftelse-hendelser request med oid: ${bruker.oid}", aktorIdent = bruker.ident, sluttbrukerIdent = request.identitetsnummer)
            brukerstoetteService.hentBekreftelseHendelser(request.identitetsnummer)
        }.onSuccess { hendelser ->
            hendelser?.let { call.respond(it) } ?: call.respond(
                HttpStatusCode.NotFound, Feil(
                    melding = "Ingen hendelser for bruker",
                    feilKode = "ikke funnet"
                )
            )
        }.onFailure {
            errorLogger.error("Feil ved henting av bekreftelse hendelser", it)
            call.respond(
                HttpStatusCode.InternalServerError, Feil(
                    melding = "Feil ved henting av bekreftelse hendelser",
                    feilKode = "intern feil"
                )
            )
        }
    }
}

fun Application.configureHTTP(
    binders: List<MeterBinder>,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    securityConfig: SecurityConfig
) {
    installAuthenticationPlugin(securityConfig.authProviders)
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
