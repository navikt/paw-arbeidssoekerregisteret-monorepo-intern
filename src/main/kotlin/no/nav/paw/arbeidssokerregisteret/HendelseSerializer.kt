package no.nav.paw.arbeidssokerregisteret

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.kafka.common.serialization.Serializer

class HendelseSerializer : Serializer<Hendelse> {
    private val objectMapper = ObjectMapper().registerKotlinModule()

    override fun serialize(topic: String?, data: Hendelse): ByteArray {
        return objectMapper.writeValueAsBytes(data)
    }
}
