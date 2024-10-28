package no.nav.paw.kafkakeygenerator.ktor

import com.fasterxml.jackson.databind.DatabindException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.api.recordkey.configureRecordKeyApi
import no.nav.paw.kafkakeygenerator.api.v2.konfigurerApiV2
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.paw.kafkakeygenerator.masker
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.LoggerFactory
import java.time.Duration

fun Application.konfigurerServer(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applikasjon: Applikasjon,
    mergeDetector: MergeDetector
) {
    autentisering(autentiseringKonfigurasjon)
    micrometerMetrics(prometheusMeterRegistry)
    configureLogging()
    serialisering()
    statusPages()
    routing {
        konfigurereHelse(
            prometheusMeterRegistry = prometheusMeterRegistry,
            mergeDetector = mergeDetector
        )
        konfigurerApiV2(autentiseringKonfigurasjon, applikasjon)
        configureRecordKeyApi(autentiseringKonfigurasjon, applikasjon)
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "docs/record-key", swaggerFile = "openapi/record-key-api-spec.yaml")
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
        distributionStatisticConfig =
            DistributionStatisticConfig.builder()
                .percentilesHistogram(true)
                .maximumExpectedValue(Duration.ofMillis(750).toNanos().toDouble())
                .minimumExpectedValue(Duration.ofMillis(20).toNanos().toDouble())
                .serviceLevelObjectives(
                    Duration.ofMillis(100).toNanos().toDouble(),
                    Duration.ofMillis(200).toNanos().toDouble()
                )
                .build()
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

fun Application.configureLogging() {
    install(CallLogging) {
        disableDefaultColors()
        filter { !it.request.path().startsWith("/internal") && it.response.status() != HttpStatusCode.OK }
    }
}