package no.nav.paw.arbeidssokerregisteret

import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.cfg.DateTimeFeature

import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.kafka.common.serialization.Serializer

class HendelseSerializer : Serializer<Hendelse> {
    private val objectMapper = JsonMapper.builder()
        // Behold Jackson 2-formatet på topic: tidspunkter som tall
        .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .addModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, true)
                .configure(KotlinFeature.NullToEmptyMap, true)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )
        .build()

    override fun serialize(topic: String?, data: Hendelse): ByteArray {
        return objectMapper.writeValueAsBytes(data)
    }
}
