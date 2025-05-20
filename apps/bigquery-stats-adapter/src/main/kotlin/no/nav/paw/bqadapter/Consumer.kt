package no.nav.paw.bqadapter

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.bqadapter.bigquery.BigQueryContext
import no.nav.paw.bqadapter.bigquery.HENDELSE_TABELL
import no.nav.paw.bqadapter.bigquery.PERIODE_TABELL
import no.nav.paw.bqadapter.bigquery.Row
import no.nav.paw.bqadapter.bigquery.schema.hendelseRad
import no.nav.paw.bqadapter.bigquery.schema.periodeRad
import org.apache.kafka.clients.consumer.ConsumerRecord

const val PERIODE_TOPIC = "paw.arbeidssokerperioder-v1"
const val HENDELSE_TOPIC = "paw.arbeidssoker-hendelseslogg-v1"

data class Record<A : Any>(
    val id: String,
    val value: A
)

fun BigQueryContext.handleRecords(records: Iterable<ConsumerRecord<Long, ByteArray>>) {
    val records = records.mapNotNull { record ->
        deserializeRecord(record.topic(), record.value())
            ?.let { deserialized ->
                Record(
                    id = encoder.encodeRecordId(
                        topic = record.topic(),
                        partition = record.partition(),
                        offset = record.offset()
                    ),
                    value = deserialized
                )
            }
    }.let(::RecordsByType)
    lagrePerioder(records.get())
    lagreHendelser(records.get())
}



class RecordsByType(source: Iterable<Record<Any>>)  {
    val map = source.groupBy { it.value::class }.withDefault { emptyList() }
    @Suppress("UNCHECKED_CAST")
    inline fun <reified A: Any> get(): Iterable<Record<A>> {
        return map[A::class] as Iterable<Record<A>>
    }
}

fun BigQueryContext.deserializeRecord(topic: String, bytes: ByteArray): Any? {
    return when (topic) {
        PERIODE_TOPIC -> periodeDeserializer.deserialize(topic, bytes)
        HENDELSE_TOPIC -> hendelseDeserializer.deserialize(topic, bytes)
        else -> null
    }
}

fun BigQueryContext.lagrePerioder(records: Iterable<Record<Periode>>) {
    records.map { record ->
        Row(id = record.id, value = periodeRad(encoder, record.value))
    }.takeIf { it.isNotEmpty() }
        ?.also { periodeRader ->
            bqDatabase.write(
                tableName = PERIODE_TABELL,
                rows = periodeRader
            )
        }
}

fun BigQueryContext.lagreHendelser(records: Iterable<Record<Hendelse>>) {
    records.mapNotNull { record ->
        Row(id = record.id, value = hendelseRad(encoder, record.value))
    }.takeIf { it.isNotEmpty() }
        ?.also { periodeRader ->
            bqDatabase.write(
                tableName = HENDELSE_TABELL,
                rows = periodeRader
            )
        }
}