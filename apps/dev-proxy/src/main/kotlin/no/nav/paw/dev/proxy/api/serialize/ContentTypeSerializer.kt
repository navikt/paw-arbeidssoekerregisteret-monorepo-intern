package no.nav.paw.dev.proxy.api.serialize

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.SerializationContext
import io.ktor.http.ContentType

class ContentTypeSerializer : ValueSerializer<ContentType>() {
    override fun serialize(value: ContentType?, generator: JsonGenerator, provider: SerializationContext) {
        if (value == null) return
        generator.writeString(value.toString())
    }
}