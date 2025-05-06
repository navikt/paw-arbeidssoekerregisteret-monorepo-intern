package no.nav.paw.arbeidssoekerregisteret.backup

import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.backup.config.AzureConfig
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

val testApplicationContext = ApplicationContext(
    hendelseKafkaConsumer = mockk(),
    azureConfig = loadNaisOrLocalConfiguration<AzureConfig>("azure_config.toml"),
    applicationConfig = mockk(),
    serverConfig = mockk(),
    securityConfig = mockk(),
    dataSource = mockk(),
    prometheusMeterRegistry = mockk(),
    partitionCount = mockk(),
    brukerstoetteService = mockk(),
    additionalMeterBinder = mockk(),
)