package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

interface Failure {
    fun system(): String
    fun code(): FailureCode
    fun exception(): Exception?
}

data class GenericFailure(
    private val system: String,
    private val code: FailureCode,
    private val exception: Exception? = null
) : Failure {
    override fun system(): String = system
    override fun code(): FailureCode = code
    override fun exception(): Exception? = exception
}

data class IdentitetFailure(
    private val system: String,
    private val code: FailureCode,
    private val exception: Exception? = null,
    private val identInformasjon: List<IdentInformasjon>
) : Failure {
    override fun system(): String = system
    override fun code(): FailureCode = code
    override fun exception(): Exception? = exception
    fun identInformasjon(): List<IdentInformasjon> = identInformasjon
}

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

suspend fun <R> Either<Failure, R>.suspendingRecover(
    failureCode: FailureCode,
    f: suspend (Failure) -> Either<Failure, R>
): Either<Failure, R> {
    return when (this) {
        is Right -> this
        is Left -> if (left.code() == failureCode) {
            f(left)
        } else {
            this
        }
    }
}

fun <R> Either<Failure, R>.recover(
    failureCode: FailureCode,
    f: (Failure) -> Either<Failure, R>
): Either<Failure, R> {
    return when (this) {
        is Right -> this
        is Left -> if (left.code() == failureCode) {
            f(left)
        } else {
            this
        }
    }
}