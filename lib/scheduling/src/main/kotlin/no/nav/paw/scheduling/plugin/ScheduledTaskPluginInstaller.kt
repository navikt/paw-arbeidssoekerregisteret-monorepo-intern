package no.nav.paw.scheduling.plugin

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import java.time.Duration

fun Application.installScheduledTaskPlugin(
    name: String = DEFAULT_SCHEDULED_TASK_NAME,
    task: (() -> Unit),
    onSuccess: ((Unit) -> Unit) = {},
    onFailure: ((throwable: Throwable) -> Unit) = {},
    onAbort: (() -> Unit) = {},
    interval: Duration,
    delay: Duration = Duration.ZERO,
    startEvent: EventDefinition<Application> = ApplicationStarted,
    stopEvent: EventDefinition<Application> = ApplicationStopping
) {
    install(ScheduledTaskPlugin(name)) {
        this.task = task
        this.onSuccess = onSuccess
        this.onFailure = onFailure
        this.onAbort = onAbort
        this.delay = delay
        this.interval = interval
        this.startEvent = startEvent
        this.stopEvent = stopEvent
    }
}
