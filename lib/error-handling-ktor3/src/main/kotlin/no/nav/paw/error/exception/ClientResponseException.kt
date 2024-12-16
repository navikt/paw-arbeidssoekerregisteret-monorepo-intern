package no.nav.paw.error.exception

import io.ktor.http.HttpStatusCode
import java.net.URI

open class ClientResponseException(
    val status: HttpStatusCode,
    override val type: URI,
    override val message: String,
    override val cause: Throwable? = null
) : ErrorTypeAwareException(type, message, cause)