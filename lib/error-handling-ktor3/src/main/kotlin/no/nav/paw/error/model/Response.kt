package no.nav.paw.error.model

import no.nav.paw.error.exception.ProblemDetailsException

sealed interface Response<out T>

data class Data<T>(val data: T) : Response<T>

fun <T> Response<T>.getOrThrow(transform: (ProblemDetails) -> ProblemDetailsException = ::ProblemDetailsException):T {
    return when(this) {
        is Data -> this.data
        is ProblemDetails -> throw transform(this)
    }
}

fun <T1, T2> Response<T1>.map(transform: (T1) -> T2): Response<T2> {
    return when(this) {
        is Data -> Data(transform(this.data))
        is ProblemDetails -> this
    }
}

fun <T1, T2> Response<T1>.flatMap(transform: (T1) -> Response<T2>): Response<T2> {
    return when(this) {
        is Data -> transform(this.data)
        is ProblemDetails -> this
    }
}


