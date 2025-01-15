package no.nav.paw.bekreftelsetjeneste.tilstand

import arrow.integrations.jackson.module.NonEmptyListModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class InternTilstandSerde : Serde<BekreftelseTilstand> {
    override fun serializer(): Serializer<BekreftelseTilstand> {
        return InternTilstandSerializer
    }

    override fun deserializer(): Deserializer<BekreftelseTilstand> {
        return InternTilstandDeserializer
    }
}

object InternTilstandSerializer : Serializer<BekreftelseTilstand> {
    override fun serialize(topic: String?, data: BekreftelseTilstand?): ByteArray {
        return internTilstandObjectMapper.writeValueAsBytes(data)
    }
}

object InternTilstandDeserializer : Deserializer<BekreftelseTilstand> {
    override fun deserialize(topic: String?, data: ByteArray?): BekreftelseTilstand {
        return internTilstandObjectMapper.readValue(data, BekreftelseTilstand::class.java)
    }
}

private val internTilstandObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModules(NonEmptyListModule)
    .registerModules(JavaTimeModule())
