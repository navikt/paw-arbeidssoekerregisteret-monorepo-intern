package no.nav.paw.arbeidssoeker.synk

import no.nav.paw.arbeidssoeker.synk.config.JOB_CONFIG
import no.nav.paw.arbeidssoeker.synk.config.JobConfig
import no.nav.paw.arbeidssoeker.synk.consumer.InngangHttpConsumer
import no.nav.paw.arbeidssoeker.synk.repository.ArbeidssoekerSynkRepository
import no.nav.paw.arbeidssoeker.synk.service.ArbeidssoekerSynkService
import no.nav.paw.arbeidssoeker.synk.utils.ArbeidssoekerCsvReader
import no.nav.paw.arbeidssoeker.synk.utils.flywayMigrate
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.logging.logger.buildApplicationLogger
import org.jetbrains.exposed.sql.Database
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.system.exitProcess

fun main() {
    val logger = buildApplicationLogger

    val jobConfig = loadNaisOrLocalConfiguration<JobConfig>(JOB_CONFIG)
    val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
    val azureAdM2MConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)

    with(jobConfig) {
        if (jobEnabled) {
            try {
                logger.info("Initialiserer jobb")

                val arbeidssoekerSynkRepository = ArbeidssoekerSynkRepository()
                val azureAdM2MTokenClient = createAzureAdM2MTokenClient(runtimeEnvironment, azureAdM2MConfig)
                val inngangHttpConsumer = InngangHttpConsumer(apiInngang.baseUrl) {
                    azureAdM2MTokenClient.createMachineToMachineToken(apiInngang.scope)
                }
                val arbeidssoekerSynkService = ArbeidssoekerSynkService(
                    jobConfig = jobConfig,
                    arbeidssoekerSynkRepository = arbeidssoekerSynkRepository,
                    inngangHttpConsumer = inngangHttpConsumer
                )
                val csvReader = ArbeidssoekerCsvReader(jobConfig)

                logger.info("Starter jobb")

                val filePath = Paths.get(jobConfig.csvFil.filsti)
                logger.info("Leser CSV-fil {} fra mappe {}", filePath.name, filePath.parent)

                val rows = csvReader.readValues(filePath)
                if (rows.hasNextValue()) {
                    val dataSource = createHikariDataSource(databaseConfig)
                    dataSource.flywayMigrate()
                    Database.connect(dataSource)

                    arbeidssoekerSynkService.synkArbeidssoekere(filePath.name, rows)
                } else {
                    logger.warn("CSV-fil {} fra mappe {} er tom", filePath.name, filePath.parent)
                }

                exitProcess(0)
            } catch (throwable: Throwable) {
                logger.error("Kjøring feilet", throwable)
                exitProcess(1)
            } finally {
                logger.info("Avslutter jobb")
            }
        } else {
            logger.info("Jobb er inaktivert")
        }
    }
}
