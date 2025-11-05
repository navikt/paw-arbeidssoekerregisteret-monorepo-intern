package no.nav.paw.error.serialize

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.ktor.http.HttpStatusCode

class HttpStatusCodeSerializer : JsonSerializer<HttpStatusCode>() {
    override fun serialize(value: HttpStatusCode, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeNumber(value.value)
    }
}