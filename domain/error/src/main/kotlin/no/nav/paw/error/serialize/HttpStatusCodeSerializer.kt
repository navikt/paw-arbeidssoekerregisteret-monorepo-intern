package no.nav.paw.error.serialize

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.SerializationContext
import io.ktor.http.HttpStatusCode

class HttpStatusCodeSerializer : ValueSerializer<HttpStatusCode>() {
    override fun serialize(value: HttpStatusCode, generator: JsonGenerator, provider: SerializationContext) {
        generator.writeNumber(value.value)
    }
}