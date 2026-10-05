package no.nav.paw.dev.proxy.api.serialize

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.SerializationContext
import io.ktor.http.Headers

class HeadersSerializer : ValueSerializer<Headers>() {
    override fun serialize(value: Headers?, generator: JsonGenerator, provider: SerializationContext) {
        if (value == null) return
        val headers = mutableMapOf<String, String?>()
        value.forEach { name, values ->
            headers[name] = values.firstOrNull()
        }
        generator.writePOJO(headers)
    }
}
