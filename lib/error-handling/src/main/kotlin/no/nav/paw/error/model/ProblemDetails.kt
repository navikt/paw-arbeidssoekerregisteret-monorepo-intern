package no.nav.paw.error.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.ktor.http.HttpStatusCode
import no.nav.paw.error.serialize.HttpStatusCodeDeserializer
import no.nav.paw.error.serialize.HttpStatusCodeSerializer

/**
 * Object som inneholder detaljer om en oppstått feilsituasjon, basert på RFC 9457.
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9457">IETF RFC 9457</a>
 */
data class ProblemDetails(
    val code: String, // Maskinelt lesbar feilkode
    val title: String,
    @JsonSerialize(using = HttpStatusCodeSerializer::class) @JsonDeserialize(using = HttpStatusCodeDeserializer::class) val status: HttpStatusCode,
    val detail: String,
    val instance: String,
    val type: String = "about:blank"
)

fun build400Error(code: String, detail: String, instance: String, type: String = "about:blank") =
    buildError(code, detail, HttpStatusCode.BadRequest, instance, type)

fun build403Error(code: String, detail: String, instance: String, type: String = "about:blank") =
    buildError(code, detail, HttpStatusCode.Forbidden, instance, type)

fun build500Error(code: String, detail: String, instance: String, type: String = "about:blank") =
    buildError(code, detail, HttpStatusCode.InternalServerError, instance, type)

fun buildError(code: String, detail: String, status: HttpStatusCode, instance: String, type: String = "about:blank") =
    ProblemDetails(
        code = code,
        title = status.description,
        status = status,
        detail = detail,
        instance = instance,
        type = type
    )