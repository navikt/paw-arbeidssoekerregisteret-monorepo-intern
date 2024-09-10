package no.nav.paw.meldeplikttjeneste.tilstand

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class InternTilstandSerde : Serde<InternTilstand> {
    override fun serializer(): Serializer<InternTilstand> {
        return InternTilstandSerializer
    }

    override fun deserializer(): Deserializer<InternTilstand> {
        return InternTilstandDeserializer
    }
}

object InternTilstandSerializer : Serializer<InternTilstand> {
    override fun serialize(topic: String?, data: InternTilstand?): ByteArray {
        return internTilstandObjectMapper.writeValueAsBytes(data)
    }
}

object InternTilstandDeserializer : Deserializer<InternTilstand> {
    override fun deserialize(topic: String?, data: ByteArray?): InternTilstand {
        return internTilstandObjectMapper.readValue(data, InternTilstand::class.java)
    }
}

private val internTilstandObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModules(JavaTimeModule())

