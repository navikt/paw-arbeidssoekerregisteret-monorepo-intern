package no.nav.paw.bekreftelseutgang.tilstand

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseDeserializer
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
    .registerModules(SimpleModule().addDeserializer(BekreftelseHendelse::class.java,
        BekreftelseHendelseJsonDeserializer
    ))
    .registerModules(JavaTimeModule())

object BekreftelseHendelseJsonDeserializer : JsonDeserializer<BekreftelseHendelse>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): BekreftelseHendelse =
        BekreftelseHendelseDeserializer.deserializeNode(context.readTree(parser))
}

