package no.nav.paw.kafkakeygenerator

data class Failure(
    val system: String,
    val code: FailureCode,
    val exception: Exception? = null
)

enum class FailureCode {
    PDL_NOT_FOUND,
    DB_NOT_FOUND,
    EXTERNAL_TECHINCAL_ERROR,
    INTERNAL_TECHINCAL_ERROR,
    CONFLICT
}

fun <R> attempt(function: () -> R): Either<Exception, R> {
    return try {
        right(function())
    } catch (ex: Exception) {
        left(ex)
    }
}

suspend fun <R> suspendeableAttempt(function: suspend () -> R): Either<Exception, R> {
    return try {
        right(function())
    } catch (ex: Exception) {
        left(ex)
    }
}


fun <R> Either<Exception, R>.mapToFailure(
    f: (Exception) -> Failure
): Either<Failure, R> {
    return when (this) {
        is Right -> this
        is Left -> left(f(left))
    }
}

suspend fun <R> Either<Failure, R>.suspendingRecover(failureCode: FailureCode, f: suspend () -> Either<Failure, R>): Either<Failure, R> {
    return when (this) {
        is Right -> this
        is Left -> if (left.code == failureCode) {
            f()
        } else {
            this
        }
    }
}

fun <R> Either<Failure, R>.recover(failureCode: FailureCode, f: () -> Either<Failure, R>): Either<Failure, R> {
    return when (this) {
        is Right -> this
        is Left -> if (left.code == failureCode) {
            f()
        } else {
            this
        }
    }
}