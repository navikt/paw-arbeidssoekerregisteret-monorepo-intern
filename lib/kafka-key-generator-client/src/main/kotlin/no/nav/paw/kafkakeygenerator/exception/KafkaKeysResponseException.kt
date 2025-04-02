package no.nav.paw.kafkakeygenerator.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ClientResponseException
import java.net.URI

open class KafkaKeysResponseException(
    override val status: HttpStatusCode,
    override val type: URI,
    override val message: String,
    override val cause: Throwable? = null
) : ClientResponseException(status, type, message, cause)