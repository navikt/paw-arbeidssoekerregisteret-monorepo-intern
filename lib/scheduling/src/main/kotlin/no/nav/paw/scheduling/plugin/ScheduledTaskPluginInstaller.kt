package no.nav.paw.scheduling.plugin

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.CoroutineDispatcher
import java.time.Duration

fun Application.installScheduledTaskPlugin(
    pluginInstance: Any,
    task: (() -> Unit),
    delay: Duration? = null,
    period: Duration? = null,
    startEvent: EventDefinition<Application>? = null,
    stopEvent: EventDefinition<Application>? = null,
    coroutineDispatcher: CoroutineDispatcher? = null
) {
    install(ScheduledTaskPlugin(pluginInstance)) {
        this.task = task
        this.delay = delay
        this.period = period
        this.startEvent = startEvent
        this.stopEvent = stopEvent
        this.coroutineDispatcher = coroutineDispatcher
    }
}
