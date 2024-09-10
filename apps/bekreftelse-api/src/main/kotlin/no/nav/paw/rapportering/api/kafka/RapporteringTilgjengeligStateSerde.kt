package no.nav.paw.rapportering.api.kafka

import no.nav.paw.rapportering.api.utils.JsonUtil
import no.nav.paw.rapportering.internehendelser.RapporteringTilgjengelig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

data class RapporteringTilgjengeligState(
    val rapporteringer: List<RapporteringTilgjengelig>
)

class RapporteringTilgjengeligStateSerde: Serde<RapporteringTilgjengeligState> {
    override fun serializer() = RapporteringTilgjengeligStateSerializer()
    override fun deserializer(): Deserializer<RapporteringTilgjengeligState> = RapporteringTilgjengeligStateDeserializer()
}

class RapporteringTilgjengeligStateSerializer : Serializer<RapporteringTilgjengeligState> {
    override fun serialize(topic: String?, data: RapporteringTilgjengeligState?): ByteArray {
        return JsonUtil.objectMapper.writeValueAsBytes(data)
    }
}

class RapporteringTilgjengeligStateDeserializer: Deserializer<RapporteringTilgjengeligState> {
    override fun deserialize(topic: String?, data: ByteArray?): RapporteringTilgjengeligState? {
        return JsonUtil.objectMapper.readValue(data, RapporteringTilgjengeligState::class.java)
    }
}
