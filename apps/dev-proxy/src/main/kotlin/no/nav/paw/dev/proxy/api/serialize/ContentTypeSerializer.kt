package no.nav.paw.dev.proxy.api.serialize

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.ktor.http.ContentType

class ContentTypeSerializer : JsonSerializer<ContentType>() {
    override fun serialize(value: ContentType?, generator: JsonGenerator, provider: SerializerProvider) {
        if (value == null) return
        generator.writeString(value.toString())
    }
}