package no.nav.paw.kafkakeygenerator

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.paw.kafkakeygenerator.config.DatabaseKonfigurasjon
import no.nav.paw.kafkakeygenerator.config.dataSource
import no.nav.paw.kafkakeygenerator.config.lastKonfigurasjon
import no.nav.paw.kafkakeygenerator.database.flywayMigrate
import no.nav.paw.kafkakeygenerator.ktor.initKtorServer
import no.nav.paw.kafkakeygenerator.pdl.PdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.pdl.opprettPdlKlient
import no.nav.paw.pdl.PdlClient
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

const val serverAuthentiseringKonfigFil = "ktor_server_autentisering.toml"
const val postgresKonfigFil = "postgres.toml"
const val pdlKlientKonfigFil = "pdl_klient.toml"
const val azureTokenKlientKonfigFil = "azure_token_klient.toml"


fun main() {
    val dataSource = lastKonfigurasjon<DatabaseKonfigurasjon>(postgresKonfigFil)
        .dataSource()
    val pdlKlient = opprettPdlKlient(
        lastKonfigurasjon(pdlKlientKonfigFil),
        lastKonfigurasjon(azureTokenKlientKonfigFil)
    )
    startApplikasjon(
        lastKonfigurasjon(serverAuthentiseringKonfigFil),
        dataSource,
        pdlKlient
    )
}

fun startApplikasjon(
    autentiseringKonfig: Autentiseringskonfigurasjon,
    dataSource: DataSource,
    pdlKlient: PdlClient
) {
    val database = Database.connect(dataSource)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    flywayMigrate(dataSource)
    initKtorServer(
        autentiseringKonfig,
        prometheusMeterRegistry,
        Applikasjon(
            KafkaKeys(database),
            PdlIdentitesTjeneste(pdlKlient)
        )
    ).start(wait = true)
}
