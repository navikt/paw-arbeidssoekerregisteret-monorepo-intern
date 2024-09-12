package no.nav.paw.error.model

import io.ktor.http.HttpStatusCode

/**
 * Object som inneholder detaljer om en oppstått feilsituasjon, basert på RFC 7807.
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">IETF RFC 7807</a>
 */
data class ProblemDetails(
    val type: String,
    val title: String,
    val status: HttpStatusCode,
    val detail: String,
    val instance: String
) {
    constructor(
        title: String,
        status: HttpStatusCode,
        detail: String,
        instance: String
    ) : this("about:blank", title, status, detail, instance)
}

fun build400Error(type: String, detail: String, instance: String) =
    buildError(type, detail, HttpStatusCode.BadRequest, instance)

fun build403Error(type: String, detail: String, instance: String) =
    buildError(type, detail, HttpStatusCode.Forbidden, instance)

fun build500Error(type: String, detail: String, instance: String) =
    buildError(type, detail, HttpStatusCode.InternalServerError, instance)

fun buildError(type: String, detail: String, status: HttpStatusCode, instance: String) = ProblemDetails(
    type = type,
    title = status.description,
    status = status,
    detail = detail,
    instance = instance
)