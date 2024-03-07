package no.nav.paw.kafkakeygenerator.ktor

import com.fasterxml.jackson.databind.DatabindException
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.api.v1.konfigurerApi
import no.nav.paw.kafkakeygenerator.api.v2.konfigurerApiV2
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.paw.kafkakeygenerator.masker
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.LoggerFactory

fun Application.konfigurerServer(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applikasjon: Applikasjon
) {
    autentisering(autentiseringKonfigurasjon)
    micrometerMetrics(prometheusMeterRegistry)
    serialisering()
    statusPages()
    routing {
        konfigurereHelse(prometheusMeterRegistry)
        konfigurerApi(autentiseringKonfigurasjon, applikasjon)
        konfigurerApiV2(autentiseringKonfigurasjon, applikasjon)
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")
    }
}

fun Application.micrometerMetrics(prometheusMeterRegistry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
        )
    }
}

fun Application.autentisering(autentiseringskonfigurasjon: Autentiseringskonfigurasjon) {
    authentication {
        autentiseringskonfigurasjon.providers.forEach { provider ->
            tokenValidationSupport(
                name = provider.name,
                requiredClaims = RequiredClaims(
                    issuer = provider.name,
                    claimMap = provider.requiredClaims.toTypedArray()
                ),
                config = TokenSupportConfig(
                    IssuerConfig(
                        name = provider.name,
                        discoveryUrl = provider.discoveryUrl,
                        acceptedAudience = provider.acceptedAudience
                    ),
                ),
            )
        }
    }
}

fun Application.serialisering() {
    install(ContentNegotiation) {
        jackson()
    }
}

private val feilLogger = LoggerFactory.getLogger("error_logger")
fun Application.statusPages() {
    install(StatusPages) {
        exception<Throwable> { call, throwable ->
            when (throwable) {
                is DatabindException -> {
                    feilLogger.info(
                        "Ugyldig kall {}, feilet, grunnet: {}",
                        masker(call.request.path()),
                        masker(throwable.message)
                    )
                    call.respondText(
                        "Bad request",
                        ContentType.Text.Plain,
                        HttpStatusCode.BadRequest
                    )
                }
                else -> {
                    feilLogger.error(
                        "Kall {}, feilet, grunnet: {}",
                        masker(call.request.path()),
                        masker(throwable.message)
                    )
                    call.respondText(
                        "En uventet feil oppstod",
                        ContentType.Text.Plain,
                        HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }
}