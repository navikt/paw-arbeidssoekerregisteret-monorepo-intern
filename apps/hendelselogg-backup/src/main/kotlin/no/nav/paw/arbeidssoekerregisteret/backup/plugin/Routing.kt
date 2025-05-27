package no.nav.paw.arbeidssoekerregisteret.backup.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.apiDocsRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.brukerstoetteRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.StartedHealthIndicator
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.metrics.route.metricsRoutes
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering
import javax.sql.DataSource

fun Application.configureRouting(applicationContext: ApplicationContext) {
    with(applicationContext) {
        install(IgnoreTrailingSlash)
        routing {
            healthRoutes(healthIndicatorRepository)
            metricsRoutes(prometheusMeterRegistry)
            apiDocsRoutes()
            route("/api/v1") {
                if (currentRuntimeEnvironment is ProdGcp) {
                    autentisering(AzureAd) {
                        brukerstoetteRoutes(brukerstoetteService)
                    }
                } else {
                    brukerstoetteRoutes(brukerstoetteService)
                }
            }
        }
    }
}

class KafkaHealthIndicator(initialStatus: HealthStatus) : StartedHealthIndicator(initialStatus) {
    //TODO: Bruke kafka error handler her? Eller kafka wrapperen sammen med isRunning og sånt?
}

private val logger = buildApplicationLogger

//TODO: Plugge på denne i healthIndicatorRepository
class DatabaseHealthIndicator(
    val dataSource: DataSource,
    initialStatus: HealthStatus,
) : StartedHealthIndicator(initialStatus) {
    fun isConnected() = runCatching {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT 1").execute()
        }
    }.onFailure { error ->
        logger.error("Db connection error", error)
    }.onSuccess {
        logger.info("Db connection successful")
    }.isSuccess
}
