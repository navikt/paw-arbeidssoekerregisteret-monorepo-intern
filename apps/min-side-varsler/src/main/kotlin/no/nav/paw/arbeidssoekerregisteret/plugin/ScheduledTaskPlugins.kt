package no.nav.paw.arbeidssoekerregisteret.plugin

import io.ktor.server.application.Application
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.service.BestillingService
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.scheduling.plugin.installScheduledTaskPlugin

private val logger = buildApplicationLogger
private const val SCHEDULED_TASK_NAME = "ManuellVarsling"

fun Application.installScheduledTaskPlugins(
    applicationConfig: ApplicationConfig,
    bestillingService: BestillingService
) {
    if (applicationConfig.manuelleVarslerEnabled) {
        installScheduledTaskPlugin(
            name = SCHEDULED_TASK_NAME,
            task = bestillingService::prosesserBestillinger,
            delay = applicationConfig.manueltVarselSchedulingDelay,
            interval = applicationConfig.manueltVarselSchedulingPeriode
        )
        logger.info("Utsendelse av manuelle varsler er aktiv")
    } else {
        logger.info("Utsendelse av manuelle varsler er deaktivert")
    }
}
