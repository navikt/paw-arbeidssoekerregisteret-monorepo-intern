package no.nav.paw.error.exception

import java.net.URI

open class ErrorTypeAwareException(
    open val type: URI,
    override val message: String,
    override val cause: Throwable? = null
) :
    Exception(message, cause) {
}