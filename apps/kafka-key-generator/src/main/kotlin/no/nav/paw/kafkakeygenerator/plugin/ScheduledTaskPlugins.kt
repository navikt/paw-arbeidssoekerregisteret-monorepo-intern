package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.service.HendelseService
import no.nav.paw.kafkakeygenerator.service.KonfliktService
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.scheduling.plugin.installScheduledTaskPlugin

private val logger = buildApplicationLogger

fun Application.installScheduledTaskPlugins(
    applicationConfig: ApplicationConfig,
    konfliktService: KonfliktService,
    hendelseService: HendelseService
) {
    with(applicationConfig) {
        if (identitetKonfliktJob.enabled) {
            installScheduledTaskPlugin(
                name = "IdentitetKonflikt",
                task = konfliktService::handleVentendeMergeKonflikter,
                delay = identitetKonfliktJob.delay,
                interval = identitetKonfliktJob.interval,
                onFailure = konfliktService::handleMergeJobbFeilet,
                onAbort = konfliktService::handleMergeJobbAvbrutt
            )
            logger.info("Jobb for håndtering av identitet-konflikter er aktiv")
        } else {
            logger.info("Jobb for håndtering av identitet-konflikter er deaktivert")
        }

        if (identitetHendelseJob.enabled) {
            installScheduledTaskPlugin(
                name = "IdentitetHendelse",
                task = hendelseService::sendVentendeIdentitetHendelser,
                delay = identitetHendelseJob.delay,
                interval = identitetHendelseJob.interval
            )
            logger.info("Jobb for utsending av identitet-hendelser er aktiv")
        } else {
            logger.info("Jobb for utsending av identitet-hendelser er deaktivert")
        }
    }
}
