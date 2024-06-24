package no.nav.paw.arbeidssokerregisteret.intern.v1

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import kotlin.reflect.KClass

class HendelseSerde : Serde<Hendelse> {
    private val objectMapper = hendelseObjectMapper()
    override fun serializer() = HendelseSerializer(objectMapper)
    override fun deserializer() = HendelseDeserializer(objectMapper)
}

class HendelseSerializer(private val objectMapper: ObjectMapper) : Serializer<Hendelse> {
    constructor() : this(hendelseObjectMapper())

    override fun serialize(topic: String?, data: Hendelse?): ByteArray {
        return data?.let {
            objectMapper.writeValueAsBytes(it)
        } ?: ByteArray(0)
    }

    fun serializeToString(data: Hendelse): String = objectMapper.writeValueAsString(data)
}

class HendelseDeserializer(private val objectMapper: ObjectMapper) : Deserializer<Hendelse> {
    constructor() : this(hendelseObjectMapper())

    override fun deserialize(topic: String?, data: ByteArray?): Hendelse? {
        if (data == null) return null
        return no.nav.paw.arbeidssokerregisteret.intern.v1.deserialize(objectMapper, data)
    }

    fun deserializeFromString(json: String): Hendelse {
        val node = objectMapper.readTree(json)
        val eventClass = eventTypeToClass(node.get("hendelseType")?.asText())
        return objectMapper.treeToValue(node, eventClass.java)
    }

}

private fun hendelseObjectMapper(): ObjectMapper = ObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
    .registerModules(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, true)
            .configure(KotlinFeature.NullToEmptyMap, true)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build(),
        JavaTimeModule()
    )

fun deserialize(objectMapper: ObjectMapper, json: ByteArray): Hendelse {
    val node = objectMapper.readTree(json)
    val eventClass =  eventTypeToClass(node.get("hendelseType")?.asText())
    return objectMapper.treeToValue(node, eventClass.java)
}

fun eventTypeToClass(type: String?): KClass<out Hendelse> =
    when (type) {
        null -> throw IllegalArgumentException("Hendelse mangler type")
        startetHendelseType -> Startet::class
        avsluttetHendelseType -> Avsluttet::class
        avvistHendelseType -> Avvist::class
        avvistStoppAvPeriodeHendelseType -> AvvistStoppAvPeriode::class
        opplysningerOmArbeidssoekerHendelseType -> OpplysningerOmArbeidssoekerMottatt::class
        else -> throw IllegalArgumentException("Ukjent hendelse type: '$type'")
    }