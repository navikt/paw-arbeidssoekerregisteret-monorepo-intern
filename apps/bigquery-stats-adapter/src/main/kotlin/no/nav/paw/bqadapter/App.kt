package no.nav.paw.bqadapter

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bqadapter.bigquery.createBigQueryContext
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes
import org.slf4j.LoggerFactory
import java.nio.file.Paths

val appLogger = LoggerFactory.getLogger("app")

val basePath = Paths.get(" /var/run/secrets/")
val periodeIdSaltPath = Paths.get("/var/run/secrets/periode_id/enc_periode")
val hendelseIdentSaltPath = Paths.get("/var/run/secrets/ident/enc-hendelse")

fun main() {
    appLogger.info("Starter app...")
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val healthIndicatorRepository = HealthIndicatorRepository()
    appLogger.info("Mounted secrets: " + basePath.toFile().listFiles().flatMap {
        if (it.isDirectory) {
            it.listFiles().toList().map { inner -> "${it.name}/$inner" }
        } else listOf(it.name)
    })
    val encoder = Encoder(
        identSalt = hendelseIdentSaltPath.toFile().readBytes(),
        periodeIdSalt = periodeIdSaltPath.toFile().readBytes()
    )
    appLogger.info("Lastet encoder: $encoder")
    val appConfig = appConfig
    appLogger.info("App config: $appConfig")
    val bigqueryContext = createBigQueryContext(
        project = appConfig.bigqueryProject
    )
    embeddedServer(factory = Netty, port = 8080) {
        routing {
            metricsRoutes(prometheusMeterRegistry)
            healthRoutes(healthIndicatorRepository)
        }
    }.start(wait = true)
}