package no.nav.paw.arbeidssoekerregisteret.backup.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.install
import io.ktor.server.application.log
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.initHwm
import no.nav.paw.database.plugin.FlywayMigrationCompleted

class HwmPluginConfig {
    var applicationContext: ApplicationContext? = null

    companion object {
        const val PLUGIN_NAME = "HwmPlugin"
    }
}

val HwmPlugin: ApplicationPlugin<HwmPluginConfig> =
    createApplicationPlugin(HwmPluginConfig.PLUGIN_NAME, ::HwmPluginConfig) {
        application.log.info("Installerer {}", HwmPluginConfig.PLUGIN_NAME)
        val applicationContext = requireNotNull(pluginConfig.applicationContext) { "applicationContext er null" }

        on(MonitoringEvent(FlywayMigrationCompleted)) { application ->
            application.log.info("Initiatlizing HWM")
            initHwm(applicationContext)
        }
    }

fun Application.installHwmPlugin(applicationContext: ApplicationContext) {
    install(HwmPlugin) {
        this.applicationContext = applicationContext
    }
}