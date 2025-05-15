package no.nav.paw.arbeidssoekerregisteret.backup

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.AzureConfig
import no.nav.paw.arbeidssoekerregisteret.backup.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.database.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.backup.database.dataSource
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.security.authentication.config.SecurityConfig

fun testApplicationContext(
    brukerstoetteService: BrukerstoetteService = mockk(relaxed = true),
): ApplicationContext {
    return testApplicationContext.copy(
        brukerstoetteService = brukerstoetteService,
    )
}

val testApplicationContext = ApplicationContext(
    hendelseKafkaConsumer = mockk(relaxed = true),
    azureConfig = loadNaisOrLocalConfiguration<AzureConfig>("azure_config.toml"),
    applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>("application_config.toml"),
    serverConfig = loadNaisOrLocalConfiguration<ServerConfig>("server_config.toml"),
    securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>("security_config.toml"),
    dataSource = loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml").dataSource(),
    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    brukerstoetteService = mockk(relaxed = true),
    additionalMeterBinder = mockk(relaxed = true),
)