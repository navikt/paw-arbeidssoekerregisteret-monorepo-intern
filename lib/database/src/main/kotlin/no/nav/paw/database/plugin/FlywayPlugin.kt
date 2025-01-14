package no.nav.paw.database.plugin

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import org.flywaydb.core.Flyway
import javax.sql.DataSource

val FlywayMigrationCompleted: EventDefinition<Application> = EventDefinition()

class FlywayPluginConfig {
    var dataSource: DataSource? = null
    var baselineOnMigrate: Boolean = true

    companion object {
        const val PLUGIN_NAME = "FlywayPlugin"
    }
}

val FlywayPlugin: ApplicationPlugin<FlywayPluginConfig> =
    createApplicationPlugin(FlywayPluginConfig.PLUGIN_NAME, ::FlywayPluginConfig) {
        application.log.info("Installerer {}", FlywayPluginConfig.PLUGIN_NAME)
        val dataSource = requireNotNull(pluginConfig.dataSource) { "DataSource er null" }
        val baselineOnMigrate = pluginConfig.baselineOnMigrate

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(baselineOnMigrate)
            .load()

        on(MonitoringEvent(DataSourceReady)) { application ->
            application.log.info("Running database migration")
            flyway.migrate()
            application.monitor.raise(FlywayMigrationCompleted, application)
        }
    }
