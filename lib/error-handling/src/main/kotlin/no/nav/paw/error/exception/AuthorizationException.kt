package no.nav.paw.error.exception

import io.ktor.http.HttpStatusCode

open class AuthorizationException(
    override val code: String,
    override val message: String,
    override val cause: Throwable? = null
) : ServerResponseException(HttpStatusCode.Forbidden, code, message, cause)