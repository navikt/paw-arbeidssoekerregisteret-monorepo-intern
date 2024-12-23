package no.nav.paw.kafkakeygenerator.plugin.custom

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.utils.io.KtorDsl
import org.flywaydb.core.Flyway
import javax.sql.DataSource

val FlywayMigrationCompleted: EventDefinition<Application> = EventDefinition()

@KtorDsl
class FlywayPluginConfig {
    var dataSource: DataSource? = null
    var baselineOnMigrate: Boolean = true

    companion object {
        const val PLUGIN_NAME = "FlywayPlugin"
    }
}

val FlywayPlugin: ApplicationPlugin<FlywayPluginConfig> =
    createApplicationPlugin(FlywayPluginConfig.PLUGIN_NAME, ::FlywayPluginConfig) {
        application.log.info("Oppretter {}", FlywayPluginConfig.PLUGIN_NAME)
        val dataSource = requireNotNull(pluginConfig.dataSource) { "DataSource er null" }
        val baselineOnMigrate = pluginConfig.baselineOnMigrate

        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.info("Running database migration")
            dataSource.flywayMigrate(baselineOnMigrate)
            application.monitor.raise(FlywayMigrationCompleted, application)
        }
    }

fun DataSource.flywayMigrate(baselineOnMigrate: Boolean = true) {
    Flyway.configure()
        .dataSource(this)
        .baselineOnMigrate(baselineOnMigrate)
        .load()
        .migrate()
}
