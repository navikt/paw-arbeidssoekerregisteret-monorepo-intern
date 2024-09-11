package no.nav.paw.bekreftelse.api.kafka

import no.nav.paw.bekreftelse.api.utils.JsonUtil
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

data class InternState(
    val tilgjendeligeBekreftelser: List<BekreftelseTilgjengelig>
)

class InternStateSerde : Serde<InternState> {
    override fun serializer() = InternStateSerializer()
    override fun deserializer(): Deserializer<InternState> =
        InternStateDeserializer()
}

class InternStateSerializer : Serializer<InternState> {
    override fun serialize(topic: String?, data: InternState?): ByteArray {
        return JsonUtil.objectMapper.writeValueAsBytes(data)
    }
}

class InternStateDeserializer : Deserializer<InternState> {
    override fun deserialize(topic: String?, data: ByteArray?): InternState? {
        return JsonUtil.objectMapper.readValue(data, InternState::class.java)
    }
}
