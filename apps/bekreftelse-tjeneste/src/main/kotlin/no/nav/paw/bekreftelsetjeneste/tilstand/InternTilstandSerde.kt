package no.nav.paw.bekreftelsetjeneste.tilstand

import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.databind.cfg.DateTimeFeature

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

private val internTilstandObjectMapper = jacksonMapperBuilder()
    // Behold Jackson 2-formatet i state store: tidspunkter som tall
    .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    .build()
