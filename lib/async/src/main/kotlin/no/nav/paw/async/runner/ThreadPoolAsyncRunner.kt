package no.nav.paw.async.runner

import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

open class ThreadPoolAsyncRunner<T>(
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor(),
    recursive: Boolean = false,
    forceCancel: Boolean = false
) : AsyncRunner<T, Future<*>> {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val keepRunning: AtomicBoolean = AtomicBoolean(recursive)
    private val mayInterruptIfRunning: AtomicBoolean = AtomicBoolean(forceCancel)
    private val futureRef: AtomicReference<Future<*>> = AtomicReference(CompletableFuture<Nothing>())

    override fun run(task: () -> T, onFailure: (Throwable) -> Unit, onSuccess: (T) -> Unit): Future<*> {
        logger.info("Running thread pool async function")
        futureRef.set(executorService.submit {
            do {
                try {
                    val result = task()
                    onSuccess(result)
                } catch (throwable: Throwable) {
                    onFailure(throwable)
                }
            } while (keepRunning.get())
        })
        return futureRef.get()
    }

    override fun abort(onAbort: () -> Unit) {
        logger.info("Aborting thread pool async function")
        keepRunning.set(false)
        futureRef.get().cancel(mayInterruptIfRunning.get())
        onAbort()
    }
}