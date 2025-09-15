package no.nav.paw.dev.proxy.api.serialize

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.ktor.http.Headers

class HeadersSerializer : JsonSerializer<Headers>() {
    override fun serialize(value: Headers?, generator: JsonGenerator, provider: SerializerProvider) {
        if (value == null) return
        val headers = mutableMapOf<String, List<String>>()
        value.forEach { name, values -> headers[name] = values }
        generator.writeObject(headers)
    }
}
