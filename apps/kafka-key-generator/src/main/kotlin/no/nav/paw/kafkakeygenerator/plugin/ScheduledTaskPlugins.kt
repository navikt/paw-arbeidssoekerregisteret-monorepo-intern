package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.service.IdentitetHendelseService
import no.nav.paw.kafkakeygenerator.service.IdentitetKonfliktService
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.scheduling.plugin.installScheduledTaskPlugin

private val logger = buildApplicationLogger

fun Application.installScheduledTaskPlugins(
    applicationConfig: ApplicationConfig,
    identitetKonfliktService: IdentitetKonfliktService,
    identitetHendelseService: IdentitetHendelseService
) {
    with(applicationConfig.identitetKonfliktJob) {
        if (enabled) {
            installScheduledTaskPlugin(
                name = "IdentitetKonflikt",
                task = identitetKonfliktService::handleVentendeIdentitetKonflikter,
                delay = delay,
                interval = interval
            )
            logger.info("Jobb for håndtering av identitet-konflikter er aktiv")
        } else {
            logger.info("Jobb for håndtering av identitet-konflikter er deaktivert")
        }
    }
    with(applicationConfig.identitetHendelseJob) {
        if (enabled) {
            installScheduledTaskPlugin(
                name = "IdentitetHendelse",
                task = identitetHendelseService::sendVentendeIdentitetHendelser,
                delay = delay,
                interval = interval
            )
            logger.info("Jobb for utsending av identitet-hendelser er aktiv")
        } else {
            logger.info("Jobb for utsending av identitet-hendelser er deaktivert")
        }
    }
}
