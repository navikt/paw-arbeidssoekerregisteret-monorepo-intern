package no.nav.paw.error.exception

import io.ktor.http.HttpStatusCode

open class ServerResponseException(
    val status: HttpStatusCode,
    override val code: String,
    override val message: String,
    override val cause: Throwable?
) : ErrorCodeAwareException(code, message, cause)