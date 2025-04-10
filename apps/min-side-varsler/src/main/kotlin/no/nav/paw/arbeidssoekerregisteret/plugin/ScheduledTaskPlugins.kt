package no.nav.paw.arbeidssoekerregisteret.plugin

import io.ktor.server.application.Application
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.service.BestillingService
import no.nav.paw.arbeidssoekerregisteret.service.OppryddingService
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.scheduling.plugin.installScheduledTaskPlugin

private val logger = buildApplicationLogger

fun Application.installScheduledTaskPlugins(
    applicationConfig: ApplicationConfig,
    bestillingService: BestillingService,
    oppryddingService: OppryddingService
) {
    if (applicationConfig.manueltVarselEnabled) {
        installScheduledTaskPlugin(
            name = "ManuellVarsling",
            task = bestillingService::prosesserBestillinger,
            delay = applicationConfig.manueltVarselSchedulingDelay,
            interval = applicationConfig.manueltVarselSchedulingInterval
        )
        logger.info("Utsendelse av manuelle varsler er aktiv")
    } else {
        logger.info("Utsendelse av manuelle varsler er deaktivert")
    }

    if (applicationConfig.oppryddingEnabled) {
        installScheduledTaskPlugin(
            name = "Opprydding",
            task = oppryddingService::prosesserOpprydding,
            delay = applicationConfig.oppryddingSchedulingDelay,
            interval = applicationConfig.oppryddingSchedulingInterval
        )
        logger.info("Opprydding er aktiv")
    } else {
        logger.info("Opprydding er deaktivert")
    }
}
