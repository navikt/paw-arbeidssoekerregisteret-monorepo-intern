package no.nav.paw.error.exception

import io.ktor.http.HttpStatusCode

open class AuthenticationException(
    override val code: String,
    override val message: String,
    override val cause: Throwable? = null
) : ServerResponseException(HttpStatusCode.Unauthorized, code, message, cause)