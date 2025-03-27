package no.nav.paw.async.task

import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ScheduledTimerTask<T>(
    private val task: () -> T,
    private val onSuccess: (T) -> Unit,
    private val onFailure: (Throwable) -> Unit,
    private val keepRunning: AtomicBoolean = AtomicBoolean(false)
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