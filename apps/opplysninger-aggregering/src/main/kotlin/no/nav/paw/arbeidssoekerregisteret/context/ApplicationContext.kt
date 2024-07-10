package no.nav.paw.arbeidssoekerregisteret.context

import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import org.slf4j.Logger

data class ApplicationContext(val logger: Logger, val properties: AppConfig)
