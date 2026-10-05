package no.nav.paw.bekreftelsetjeneste.paavegneav

import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.databind.cfg.DateTimeFeature

import tools.jackson.module.kotlin.readValue
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

private val bekreftelsePaaVegneAvObjectMapper = jacksonMapperBuilder()
    // Behold Jackson 2-formatet i state store: tidspunkter som tall
    .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    .build()

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