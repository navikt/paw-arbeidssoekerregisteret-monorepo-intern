package no.nav.paw.aareg

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.reflect.KFunction

object MockResponse {
    val arbeidsforhold = "aareg-arbeidsforhold.json".readResource()

    val error = "error.json".readResource()
}
fun mockAaregClient(content: String, statusCode: HttpStatusCode = HttpStatusCode.OK): AaregClient {
    val mockEngine = MockEngine {
        respond(
            content = content,
            status = statusCode,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )
    }

    val mockHttpClient = HttpClient(mockEngine) { configureJsonHandler() }

    return mockFn(::createHttpClient) {
        every { createHttpClient() } returns mockHttpClient
        AaregClient("url") { "fake token" }
    }
}

private fun <T> mockFn(fn: KFunction<*>, block: () -> T): T {
    mockkStatic(fn)
    return try {
        block()
    } finally {
        unmockkStatic(fn)
    }
}
