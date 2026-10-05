package no.nav.paw.bekreftelseutgang.tilstand

import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.databind.cfg.DateTimeFeature

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.module.SimpleModule
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

private val internTilstandObjectMapper = jacksonMapperBuilder()
    // Behold Jackson 2-formatet i state store: tidspunkter som tall
    .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    .addModule(
        SimpleModule().addDeserializer(
            BekreftelseHendelse::class.java,
            BekreftelseHendelseJsonDeserializer
        )
    )
    .build()

object BekreftelseHendelseJsonDeserializer : ValueDeserializer<BekreftelseHendelse>() {
    private val deserializer = BekreftelseHendelseDeserializer()
    override fun deserialize(parser: JsonParser, context: DeserializationContext): BekreftelseHendelse =
        deserializer.deserializeNode(context.readTree(parser))
}

