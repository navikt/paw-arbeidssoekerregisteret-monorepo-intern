package no.nav.paw.kafkakeygenerator

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.paw.kafkakeygenerator.config.DatabaseKonfigurasjon
import no.nav.paw.kafkakeygenerator.config.dataSource
import no.nav.paw.kafkakeygenerator.config.lastKonfigurasjon
import no.nav.paw.kafkakeygenerator.database.flywayMigrate
import no.nav.paw.kafkakeygenerator.pdl.opprettPdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.webserver.initKtorServer
import org.jetbrains.exposed.sql.Database

fun main() {
    val autentiseringKonfig: Autentiseringskonfigurasjon = lastKonfigurasjon("ktor_server_autentisering.toml")
    val dataSource =
        lastKonfigurasjon<DatabaseKonfigurasjon>("postgres.toml")
            .dataSource()
    val database = Database.connect(dataSource)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    flywayMigrate(dataSource)
    initKtorServer(
        autentiseringKonfig,
        prometheusMeterRegistry,
        Applikasjon(
            ExposedKafkaKeys(database),
            opprettPdlIdentitesTjeneste()
        )).start(wait = true)
}
