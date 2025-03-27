package no.nav.paw.async.runner

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

open class CoroutineAsyncRunner<T>(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val keepRunning: AtomicBoolean = AtomicBoolean(true)
) : AsyncRunner<T, Job> {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val jobRef: AtomicReference<Job> = AtomicReference(Job())

    override fun run(task: () -> T, onFailure: (Throwable) -> Unit, onSuccess: (T) -> Unit): Job {
        logger.info("Running coroutine async function")
        jobRef.set(coroutineScope.launch(coroutineDispatcher) {
            do {
                try {
                    val result = task()
                    onSuccess(result)
                } catch (throwable: Throwable) {
                    onFailure(throwable)
                }
            } while (keepRunning.get())
        })
        return jobRef.get()
    }

    override fun abort(onAbort: () -> Unit) {
        logger.info("Aborting coroutine async function")
        keepRunning.set(false)
        jobRef.get().cancel()
        onAbort()
    }
}