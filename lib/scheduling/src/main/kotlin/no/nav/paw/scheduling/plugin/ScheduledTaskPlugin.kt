package no.nav.paw.scheduling.plugin

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import no.nav.paw.async.runner.ScheduledAsyncRunner
import java.time.Duration

private const val PLUGIN_NAME_SUFFIX = "ScheduledTaskPlugin"

class ScheduledTaskPluginConfig {
    var task: (() -> Unit)? = null
    var interval: Duration? = null
    var delay: Duration? = null
    var startEvent: EventDefinition<Application>? = null
    var stopEvent: EventDefinition<Application>? = null
}

@Suppress("FunctionName")
fun ScheduledTaskPlugin(pluginInstance: Any): ApplicationPlugin<ScheduledTaskPluginConfig> {
    val pluginName = "${pluginInstance}${PLUGIN_NAME_SUFFIX}"
    return createApplicationPlugin(pluginName, ::ScheduledTaskPluginConfig) {
        application.log.info("Installerer {}", pluginName)
        val task = requireNotNull(pluginConfig.task) { "Task er null" }
        val interval = requireNotNull(pluginConfig.interval) { "Interval er null" }
        val delay = requireNotNull(pluginConfig.delay) { "Delay er null" }
        val startEvent = pluginConfig.startEvent ?: ApplicationStarted
        val stopEvent = pluginConfig.stopEvent ?: ApplicationStopping
        val asyncRunner = ScheduledAsyncRunner<Unit>(interval, delay)

        on(MonitoringEvent(startEvent)) {
            asyncRunner.run(task, {}) {}
        }

        on(MonitoringEvent(stopEvent)) {
            asyncRunner.abort {}
        }
    }
}
