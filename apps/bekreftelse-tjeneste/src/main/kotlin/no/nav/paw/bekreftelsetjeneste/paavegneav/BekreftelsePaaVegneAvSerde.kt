package no.nav.paw.bekreftelsetjeneste.paavegneav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

private val bekreftelsePaaVegneAvObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModules(JavaTimeModule())

class BekreftelsePaaVegneAvSerde: Serde<PaaVegneAvTilstand> {
    private val bekreftelsePaaVegneAvSerializer = BekreftelsePaaVegneAvSerializer()
    private val bekreftelsePaaVegneAvDeserializer = BekreftelsePaaVegneAvDeserializer()

    override fun serializer(): Serializer<PaaVegneAvTilstand> = bekreftelsePaaVegneAvSerializer

    override fun deserializer(): Deserializer<PaaVegneAvTilstand> = bekreftelsePaaVegneAvDeserializer
}

class BekreftelsePaaVegneAvSerializer: Serializer<PaaVegneAvTilstand> {
    override fun serialize(topic: String?, data: PaaVegneAvTilstand): ByteArray {
        return bekreftelsePaaVegneAvObjectMapper.writeValueAsBytes(data)
    }
}

class BekreftelsePaaVegneAvDeserializer: Deserializer<PaaVegneAvTilstand> {
    override fun deserialize(topic: String?, data: ByteArray): PaaVegneAvTilstand {
        return bekreftelsePaaVegneAvObjectMapper.readValue(data)
    }
}