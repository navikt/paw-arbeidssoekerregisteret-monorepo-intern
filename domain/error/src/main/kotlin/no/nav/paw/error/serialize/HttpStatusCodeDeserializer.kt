package no.nav.paw.error.serialize

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import io.ktor.http.HttpStatusCode

class HttpStatusCodeDeserializer : ValueDeserializer<HttpStatusCode>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): HttpStatusCode {
        return HttpStatusCode.fromValue(parser.numberValue.toInt())
    }
}