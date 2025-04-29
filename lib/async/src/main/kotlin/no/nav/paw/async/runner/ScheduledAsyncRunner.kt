package no.nav.paw.async.runner

import no.nav.paw.async.task.ScheduledTimerTask
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

open class ScheduledAsyncRunner<T>(
    private val interval: Duration,
    private val delay: Duration = Duration.ZERO,
    private val keepRunning: AtomicBoolean = AtomicBoolean(false)
) : AsyncRunner<T, Unit> {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val timer = Timer()

    override fun run(onRun: () -> Unit) {
        logger.info(
            "Starting scheduled {}async runner with interval {} and delay {}",
            if (keepRunning.get()) "recursive " else "",
            interval,
            delay
        )
        val timerTask = ScheduledTimerTask<Void>(onRun = onRun, keepRunning = keepRunning)
        timer.scheduleAtFixedRate(timerTask, delay.toMillis(), interval.toMillis())
    }

    override fun run(task: () -> T, onFailure: (Throwable) -> Unit, onSuccess: (T) -> Unit) {
        run {
            try {
                val result = task()
                onSuccess(result)
            } catch (throwable: Throwable) {
                onFailure(throwable)
            }
        }
    }

    override fun abort(onAbort: () -> Unit) {
        logger.info("Aborting scheduled async runner")
        keepRunning.set(false)
        timer.cancel()
        onAbort()
    }
}