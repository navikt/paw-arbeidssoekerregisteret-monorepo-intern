package no.nav.paw.error.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.ktor.http.HttpStatusCode
import no.nav.paw.error.serialize.HttpStatusCodeDeserializer
import no.nav.paw.error.serialize.HttpStatusCodeSerializer
import java.net.URI
import java.time.Instant
import java.util.*

/**
 * Object som inneholder detaljer om en oppstått feilsituasjon, basert på RFC 9457.
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9457">IETF RFC 9457</a>
 */
data class ProblemDetails(
    val id: UUID = UUID.randomUUID(),
    val type: URI = ErrorType.default().build(),
    @field:JsonSerialize(using = HttpStatusCodeSerializer::class) @field:JsonDeserialize(using = HttpStatusCodeDeserializer::class) val status: HttpStatusCode,
    val title: String,
    val detail: String? = null,
    val instance: String,
    val timestamp: Instant = Instant.now()
) : Response<Nothing>

class ProblemDetailsBuilder private constructor(
    private var type: URI = ErrorType.default().build(),
    private var status: HttpStatusCode = HttpStatusCode.InternalServerError,
    private var title: String? = null,
    private var detail: String? = null,
    private var instance: String = "/"
) {
    fun type(type: URI) = apply { this.type = type }
    fun status(status: HttpStatusCode) = apply { this.status = status }
    fun title(title: String) = apply { this.title = title }
    fun detail(detail: String) = apply { this.detail = detail }
    fun instance(instance: String) = apply { this.instance = instance }
    fun build(): ProblemDetails = ProblemDetails(
        type = type,
        status = status,
        title = title ?: status.description,
        detail = detail,
        instance = instance
    )

    companion object {
        fun builder(): ProblemDetailsBuilder = ProblemDetailsBuilder()
    }
}
