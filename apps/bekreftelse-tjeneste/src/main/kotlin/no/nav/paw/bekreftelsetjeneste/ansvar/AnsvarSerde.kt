package no.nav.paw.bekreftelsetjeneste.ansvar

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

private val ansvarObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModules(JavaTimeModule())

class AnsvarSerde: Serde<Ansvar> {
    private val ansvarSerializer = AnsvarSerializer()
    private val ansvarDeserializer = AnsvarDeserializer()

    override fun serializer(): Serializer<Ansvar> = ansvarSerializer

    override fun deserializer(): Deserializer<Ansvar> = ansvarDeserializer
}

class AnsvarSerializer: Serializer<Ansvar> {
    override fun serialize(topic: String?, data: Ansvar): ByteArray {
        return ansvarObjectMapper.writeValueAsBytes(data)
    }
}

class AnsvarDeserializer: Deserializer<Ansvar> {
    override fun deserialize(topic: String?, data: ByteArray): Ansvar {
        return ansvarObjectMapper.readValue(data)
    }
}