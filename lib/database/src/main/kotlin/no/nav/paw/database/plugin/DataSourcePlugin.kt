package no.nav.paw.database.plugin

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

val DataSourceReady: EventDefinition<Application> = EventDefinition()

class DataSourcePluginConfig {
    var dataSource: DataSource? = null

    companion object {
        const val PLUGIN_NAME = "DataSourcePlugin"
    }
}

val DataSourcePlugin: ApplicationPlugin<DataSourcePluginConfig> =
    createApplicationPlugin(DataSourcePluginConfig.PLUGIN_NAME, ::DataSourcePluginConfig) {
        application.log.info("Installerer {}", DataSourcePluginConfig.PLUGIN_NAME)
        val dataSource = requireNotNull(pluginConfig.dataSource) { "DataSource er null" }

        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.info("Initializing data source")
            Database.connect(dataSource)
            application.monitor.raise(DataSourceReady, application)
        }

        on(MonitoringEvent(ApplicationStopping)) { application ->
            application.log.info("Closing data source")
            dataSource.connection.close()
        }
    }
