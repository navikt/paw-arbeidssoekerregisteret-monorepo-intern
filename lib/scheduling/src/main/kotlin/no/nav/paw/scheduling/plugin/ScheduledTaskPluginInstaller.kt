package no.nav.paw.scheduling.plugin

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import java.time.Duration

fun Application.installScheduledTaskPlugin(
    pluginInstance: Any,
    task: (() -> Unit),
    interval: Duration,
    delay: Duration = Duration.ZERO,
    startEvent: EventDefinition<Application> = ApplicationStarted,
    stopEvent: EventDefinition<Application> = ApplicationStopping
) {
    install(ScheduledTaskPlugin(pluginInstance)) {
        this.task = task
        this.delay = delay
        this.interval = interval
        this.startEvent = startEvent
        this.stopEvent = stopEvent
    }
}
