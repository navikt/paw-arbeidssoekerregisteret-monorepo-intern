package no.nav.paw.error.serialize

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import io.ktor.http.HttpStatusCode

class HttpStatusCodeDeserializer : JsonDeserializer<HttpStatusCode>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): HttpStatusCode {
        return HttpStatusCode.fromValue(parser.numberValue.toInt())
    }
}