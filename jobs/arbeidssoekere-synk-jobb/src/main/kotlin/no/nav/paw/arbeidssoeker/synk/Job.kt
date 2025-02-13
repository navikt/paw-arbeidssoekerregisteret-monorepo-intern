package no.nav.paw.arbeidssoeker.synk

import no.nav.paw.arbeidssoeker.synk.config.JOB_CONFIG
import no.nav.paw.arbeidssoeker.synk.config.JobConfig
import no.nav.paw.arbeidssoeker.synk.repository.ArbeidssoekerSynkRepository
import no.nav.paw.arbeidssoeker.synk.service.ArbeidssoekerSynkService
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.logging.logger.buildApplicationLogger
import org.jetbrains.exposed.sql.Database
import java.nio.file.Paths

fun main() {
    val logger = buildApplicationLogger
    val jobConfig = loadNaisOrLocalConfiguration<JobConfig>(JOB_CONFIG)
    val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)

    val dataSource = createHikariDataSource(databaseConfig)
    Database.connect(dataSource)
    val arbeidssoekerSynkRepository = ArbeidssoekerSynkRepository()
    val arbeidssoekerSynkService = ArbeidssoekerSynkService(arbeidssoekerSynkRepository)

    val name = jobConfig.runtimeEnvironment.appNameOrDefaultForLocal(default = "local-job")

    try {
        logger.info("Starter $name")
        arbeidssoekerSynkService.synkArbeidssoekere(Paths.get(jobConfig.mountPath))
    } catch (throwable: Throwable) {
        logger.error("Kjøring feilet", throwable)
    } finally {
        logger.info("Avslutter $name")
    }
}
