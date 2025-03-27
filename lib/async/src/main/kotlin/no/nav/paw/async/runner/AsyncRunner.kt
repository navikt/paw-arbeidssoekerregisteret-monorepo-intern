package no.nav.paw.async.runner

interface AsyncRunner<T, R> {
    fun run(task: () -> T, onFailure: (Throwable) -> Unit, onSuccess: (T) -> Unit): R
    fun abort(onAbort: () -> Unit)
}