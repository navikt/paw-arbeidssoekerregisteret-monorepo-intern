package no.nav.paw.arbeidssokerregisteret

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.config.AutentiseringsKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.config.DatabaseKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.config.connect
import no.nav.paw.arbeidssokerregisteret.config.migrateDatabase
import no.nav.paw.arbeidssokerregisteret.plugins.configureAuthentication
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureLogging
import no.nav.paw.arbeidssokerregisteret.plugins.configureMetrics
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.apiRoutes
import no.nav.paw.arbeidssokerregisteret.routes.healthRoutes
import no.nav.paw.arbeidssokerregisteret.routes.swaggerRoutes

fun main() {
    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)

    server.addShutdownHook {
        server.stop(300, 300)
    }
}

fun Application.module() {
    // Konfigurasjon
    val environment = System.getenv()
    val databaseKonfigurasjon = DatabaseKonfigurasjon(environment)
    val autentiseringsKonfigurasjon = AutentiseringsKonfigurasjon(environment)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    // Koble til database
    val arbeidssokerDatabase = databaseKonfigurasjon.connect()

    // Kjører migrering av databasen
    databaseKonfigurasjon.migrateDatabase()

    // Konfigurer plugins
    configureMetrics(prometheusMeterRegistry)
    configureHTTP()
    configureAuthentication(autentiseringsKonfigurasjon.authenticationProviders)
    configureLogging()
    configureSerialization()

    // Routes
    routing {
        healthRoutes(prometheusMeterRegistry)
        swaggerRoutes()
        apiRoutes(arbeidssokerDatabase)
    }
}
