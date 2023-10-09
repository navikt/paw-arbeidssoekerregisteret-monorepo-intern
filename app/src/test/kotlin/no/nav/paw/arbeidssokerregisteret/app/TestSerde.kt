package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import java.nio.ByteBuffer
import java.util.*

/**
 * Hele denne filen er en eneste stor workaround i påvente av at vi finner ut hvordan vi får
 * Avro med SchemaRegistry til å fungere i test.
 * Serde som kan (de)serialisere både Hendelse og PeriodeTilstandV1.
 */
class GenericSerde<T : SpecificRecord>() : Serde<T> {
    private val hendelseSerde = HendelseSerde()
    private val periodeSerde = PeriodeSerde()
    override fun serializer(): Serializer<T> {
        return Serializer { _, data ->
            val dataBytes = when (data) {
                is Hendelse -> hendelseSerde.serializer().serialize("", data)
                is PeriodeTilstandV1 -> periodeSerde.serializer().serialize("", data)
                else -> throw IllegalArgumentException("Ukjent type ${data::class.java}")
            }
            val bytes = LinkedList<Byte>()
            if (data is Hendelse) {
                bytes.add(0)
            } else {
                bytes.add(1)
            }
            bytes.addAll(dataBytes.asList())
            bytes.toByteArray()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserializer(): Deserializer<T> {
        return Deserializer { _, data ->
            when (data[0].toInt()) {
                0 -> hendelseSerde.deserializer().deserialize("", data.copyOfRange(1, data.size)) as T
                1 -> periodeSerde.deserializer().deserialize("", data.copyOfRange(1, data.size)) as T
                else -> throw IllegalArgumentException("Ukjent type ${data[0]}")
            }
        }
    }

}

class HendelseSerde() : Serde<Hendelse> {

    init {
        super.configure(emptyMap<String, Any>(), false)
    }

    override fun serializer(): Serializer<Hendelse> {
        return Serializer { _, data -> data.toByteBuffer().array() }
    }

    override fun deserializer(): Deserializer<Hendelse> {
        return Deserializer { _, data -> Hendelse.fromByteBuffer(ByteBuffer.wrap(data)) }
    }

}

class PeriodeSerde() : Serde<PeriodeTilstandV1> {

    init {
        super.configure(emptyMap<String, Any>(), false)
    }

    override fun serializer(): Serializer<PeriodeTilstandV1> {
        return Serializer { _, data -> data.toByteBuffer().array() }
    }

    override fun deserializer(): Deserializer<PeriodeTilstandV1> {
        return Deserializer { _, data -> PeriodeTilstandV1.fromByteBuffer(ByteBuffer.wrap(data)) }
    }

}
