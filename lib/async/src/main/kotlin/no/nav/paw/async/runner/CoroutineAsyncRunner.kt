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
    private val recursive: Boolean = false
) : AsyncRunner<T, Job> {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val keepRunning: AtomicBoolean = AtomicBoolean(recursive)
    private val jobRef: AtomicReference<Job> = AtomicReference(Job())

    override fun run(task: () -> T, onFailure: (Throwable) -> Unit, onSuccess: (T) -> Unit): Job {
        logger.info("Starting {}coroutine async runner", if (recursive) "recursive " else "")
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
        logger.info("Aborting coroutine async runner")
        keepRunning.set(false)
        jobRef.get().cancel()
        onAbort()
    }
}