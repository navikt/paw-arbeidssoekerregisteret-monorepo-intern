package no.nav.paw.arbeidssoeker.synk

import no.nav.paw.arbeidssoeker.synk.config.JOB_CONFIG
import no.nav.paw.arbeidssoeker.synk.config.JobConfig
import no.nav.paw.arbeidssoeker.synk.service.SyncService
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.logging.logger.buildApplicationLogger

fun main() {
    val logger = buildApplicationLogger
    val jobConfig = loadNaisOrLocalConfiguration<JobConfig>(JOB_CONFIG)
    val syncService = SyncService(jobConfig)

    val name = jobConfig.runtimeEnvironment.appNameOrDefaultForLocal(default = "local-job")

    try {
        logger.info("Starter $name")
        syncService.syncArbeidssoekere()
    } catch (throwable: Throwable) {
        logger.error("Kjøring feilet", throwable)
    } finally {
        logger.info("Stopper $name")
    }
}
