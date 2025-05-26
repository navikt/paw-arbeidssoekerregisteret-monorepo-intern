package no.nav.paw.arbeidssoekerregisteret.backup.plugin

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.apiDocsRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.brukerstoetteRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.metrics.route.metricsRoutes
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering
import javax.sql.DataSource
import kotlin.use

fun Application.configureRouting(
    applicationContext: ApplicationContext,
) {
    install(IgnoreTrailingSlash)
    routing {
        startupRoutes(applicationContext)
        healthRoutes()
        metricsRoutes(applicationContext.prometheusMeterRegistry)
        apiDocsRoutes()
        route("/api/v1") {
            if (currentRuntimeEnvironment is ProdGcp) {
                autentisering(AzureAd) {
                    brukerstoetteRoutes(applicationContext.brukerstoetteService)
                }
            } else {
                brukerstoetteRoutes(applicationContext.brukerstoetteService)
            }
        }
    }
}

private val logger = buildApplicationLogger

fun Route.startupRoutes(
    applicationContext: ApplicationContext
) {
    with(applicationContext) {
        get("/internal/startup") {
            val kafkaOk = hendelseConsumerWrapper.isRunning()
            val canConnectToDb = isConnected(dataSource)
            if (!canConnectToDb) {
                call.respondText(
                    "Application is not ready to receive requests, database connection failed",
                    status = HttpStatusCode.InternalServerError,
                    contentType = ContentType.Text.Plain
                )
            } else if(!kafkaOk) {
                call.respondText(
                    "Application is not ready to receive requests, kafka consumer connection failed",
                    status = HttpStatusCode.InternalServerError,
                    contentType = ContentType.Text.Plain
                )
            } else {
                call.respondText(
                    "Application is started and ready to receive requests",
                    status = HttpStatusCode.OK,
                    contentType = ContentType.Text.Plain
                )
            }
        }
    }
}

fun isConnected(dataSource: DataSource) = runCatching {
    dataSource.connection.use { conn ->
        conn.prepareStatement("SELECT 1").execute()
    }
}.onFailure { error ->
    logger.error("Db connection error", error)
}.onSuccess {
    logger.info("Db connection successful")
}.isSuccess