package no.nav.paw

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.paw.pdl.PdlClient

fun mockPdlClient(content: String): PdlClient {
    val mockEngine =
        MockEngine {
            respond(
                content = content,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
    return PdlClient("https://url", "tema", HttpClient(mockEngine)) { "fake token" }
}
