package no.nav.paw.async.runner

import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

open class ScheduledAsyncRunner<T>(
    private val interval: Duration,
    private val delay: Duration = Duration.ZERO,
    private val keepRunning: AtomicBoolean = AtomicBoolean(true)
) : AsyncRunner<T, Unit> {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val timer = Timer()

    override fun run(task: () -> T, onFailure: (Throwable) -> Unit, onSuccess: (T) -> Unit) {
        logger.info("Starting scheduled async function with interval {} and delay {}", interval, delay)
        val timerTask = ScheduledTimerTask(task, onSuccess, onFailure, keepRunning)
        timer.scheduleAtFixedRate(timerTask, delay.toMillis(), interval.toMillis())
    }

    override fun abort(onAbort: () -> Unit) {
        logger.info("Aborting scheduled async function")
        keepRunning.set(false)
        timer.cancel()
        onAbort()
    }

    class ScheduledTimerTask<T>(
        private val task: () -> T,
        private val onSuccess: (T) -> Unit,
        private val onFailure: (Throwable) -> Unit,
        private val keepRunning: AtomicBoolean = AtomicBoolean(true)
    ) : TimerTask() {
        private val logger = LoggerFactory.getLogger(this.javaClass)

        override fun run() {
            logger.info("Running scheduled async function")
            do {
                try {
                    val result = task()
                    onSuccess(result)
                } catch (throwable: Throwable) {
                    onFailure(throwable)
                }
            } while (keepRunning.get())
        }
    }
}