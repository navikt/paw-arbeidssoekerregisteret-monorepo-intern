package no.nav.paw.arbeidssoekerregisteret.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.utils.io.KtorDsl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.service.BestillingService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.scheduling")
private const val INSTANCE_NAME = "ManuellVarslingScheduledTaskPlugin"
private const val PLUGIN_NAME = "${INSTANCE_NAME}ScheduledTaskPlugin"

fun Application.installScheduledTaskPlugin(
    applicationConfig: ApplicationConfig,
    bestillingService: BestillingService
) {
    if (applicationConfig.manuelleVarslerEnabled) {
        installScheduledTaskPlugin(
            task = bestillingService::prosesserBestillinger,
            delay = applicationConfig.manueltVarselSchedulingDelay,
            period = applicationConfig.manueltVarselSchedulingPeriode
        )
        logger.info("Utsendelse av manuelle varsler er aktiv")
    } else {
        logger.info("Utsendelse av manuelle varsler er deaktivert")
    }
}

fun Application.installScheduledTaskPlugin(
    task: (() -> Unit),
    delay: Duration,
    period: Duration
) {
    install(ScheduledTaskPlugin) {
        this.task = task
        this.delay = delay
        this.period = period
    }
}

val ScheduledTaskPlugin: ApplicationPlugin<ScheduledTaskPluginConfig> =
    createApplicationPlugin(PLUGIN_NAME, ::ScheduledTaskPluginConfig) {
        application.log.info("Installerer {}", PLUGIN_NAME)
        val task = requireNotNull(pluginConfig.task) { "Task er null" }
        val delay = requireNotNull(pluginConfig.delay) { "Delay er null" }
        val period = requireNotNull(pluginConfig.period) { "Period er null" }
        val taskScheduler = CoroutineTaskScheduler(delay, period, Dispatchers.IO)
        var scheduledTask: ScheduledTask? = null

        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.info("Scheduling task {} at delay: {}, period {}", INSTANCE_NAME, delay, period)
            scheduledTask = taskScheduler.run(application, task)
        }

        on(MonitoringEvent(ApplicationStopping)) { _ ->
            application.log.info("Canceling scheduled task {}", INSTANCE_NAME)
            scheduledTask?.cancel()
        }
    }

@KtorDsl
class ScheduledTaskPluginConfig {
    var task: (() -> Unit)? = null
    var delay: Duration? = null
    var period: Duration? = null
}

interface ScheduledTask {
    fun cancel()
}

interface TaskScheduler {
    fun run(application: Application, task: () -> Unit): ScheduledTask
}

class CoroutineTask(private val job: Job) : ScheduledTask {
    override fun cancel() {
        job.cancel()
    }
}

class CoroutineTaskScheduler(
    private var delay: Duration,
    private var period: Duration,
    private var coroutineDispatcher: CoroutineDispatcher
) : TaskScheduler {
    override fun run(application: Application, task: () -> Unit): ScheduledTask {
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                logger.info("Running scheduled task for {}", INSTANCE_NAME)
                task()
            }
        }
        val job = application.launch(coroutineDispatcher) {
            timer.scheduleAtFixedRate(timerTask, delay.toMillis(), period.toMillis())
        }
        return CoroutineTask(job)
    }
}
