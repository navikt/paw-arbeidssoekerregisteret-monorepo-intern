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

const val DEFAULT_SCHEDULED_TASK_NAME = ""
private const val SCHEDULED_TASK_PLUGIN_SUFFIX = "ScheduledTaskPlugin"

class ScheduledTaskPluginConfig {
    var task: (() -> Unit)? = null
    var onSuccess: ((Unit) -> Unit) = {}
    var onFailure: ((throwable: Throwable) -> Unit) = {}
    var onAbort: (() -> Unit) = {}
    var interval: Duration? = null
    var delay: Duration = Duration.ZERO
    var startEvent: EventDefinition<Application> = ApplicationStarted
    var stopEvent: EventDefinition<Application> = ApplicationStopping
}

@Suppress("FunctionName")
fun ScheduledTaskPlugin(
    name: String = DEFAULT_SCHEDULED_TASK_NAME
): ApplicationPlugin<ScheduledTaskPluginConfig> {
    val pluginName = "$name$SCHEDULED_TASK_PLUGIN_SUFFIX"
    return createApplicationPlugin(pluginName, ::ScheduledTaskPluginConfig) {
        application.log.info("Installerer {}", pluginName)
        val task = requireNotNull(pluginConfig.task) { "Task er null" }
        val interval = requireNotNull(pluginConfig.interval) { "Interval er null" }
        val asyncRunner = ScheduledAsyncRunner<Unit>(interval, pluginConfig.delay)

        on(MonitoringEvent(pluginConfig.startEvent)) {
            asyncRunner.run(
                task = task,
                onFailure = pluginConfig.onFailure,
                onSuccess = pluginConfig.onSuccess
            )
        }

        on(MonitoringEvent(pluginConfig.stopEvent)) {
            asyncRunner.abort(onAbort = pluginConfig.onAbort)
        }
    }
}
