package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.service.KonfliktService
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.scheduling.plugin.installScheduledTaskPlugin

private val logger = buildApplicationLogger

fun Application.installScheduledTaskPlugins(
    applicationConfig: ApplicationConfig,
    konfliktService: KonfliktService
) {
    with(applicationConfig) {
        if (identitetMergeKonfliktJob.enabled) {
            installScheduledTaskPlugin(
                name = "IdentitetMergeKonflikt",
                task = konfliktService::handleVentendeMergeKonflikter,
                delay = identitetMergeKonfliktJob.delay,
                interval = identitetMergeKonfliktJob.interval,
                onFailure = konfliktService::handleMergeJobbFeilet,
                onAbort = konfliktService::handleMergeJobbAvbrutt
            )
            logger.info("Jobb for håndtering av {}-konflikter er aktiv", KonfliktType.MERGE.name)
        } else {
            logger.info("Jobb for håndtering av {}-konflikter er deaktivert", KonfliktType.MERGE.name)
        }

        if (identitetSplittKonfliktJob.enabled) {
            installScheduledTaskPlugin(
                name = "IdentitetSplittKonflikt",
                task = {},
                delay = identitetSplittKonfliktJob.delay,
                interval = identitetSplittKonfliktJob.interval
            )
            logger.info("Jobb for håndtering av {}-konflikter er aktiv", KonfliktType.SPLITT.name)
        } else {
            logger.info("Jobb for håndtering av {}-konflikter er deaktivert", KonfliktType.SPLITT.name)
        }
    }
}
