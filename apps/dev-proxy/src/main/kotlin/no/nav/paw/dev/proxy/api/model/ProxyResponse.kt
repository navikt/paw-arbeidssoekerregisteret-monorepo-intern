package no.nav.paw.dev.proxy.api.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import no.nav.paw.dev.proxy.api.serialize.ContentTypeSerializer
import no.nav.paw.dev.proxy.api.serialize.HeadersSerializer
import no.nav.paw.error.serialize.HttpStatusCodeSerializer

data class ProxyResponse(
    val method: String,
    val url: String,
    @field:JsonSerialize(using = HttpStatusCodeSerializer::class)
    val status: HttpStatusCode,
    @field:JsonSerialize(using = ContentTypeSerializer::class)
    val contentType: ContentType? = null,
    @field:JsonSerialize(using = HeadersSerializer::class)
    val headers: Headers? = null,
    val body: JsonNode? = null,
)
