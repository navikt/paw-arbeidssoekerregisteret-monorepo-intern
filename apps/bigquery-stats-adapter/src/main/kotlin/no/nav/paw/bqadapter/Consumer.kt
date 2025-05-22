package no.nav.paw.bqadapter

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.bqadapter.bigquery.AppContext
import no.nav.paw.bqadapter.bigquery.HENDELSE_TABELL
import no.nav.paw.bqadapter.bigquery.PERIODE_TABELL
import no.nav.paw.bqadapter.bigquery.Row
import no.nav.paw.bqadapter.bigquery.schema.hendelseRad
import no.nav.paw.bqadapter.bigquery.schema.periodeRad
import no.nav.paw.health.model.HealthStatus
import org.apache.kafka.clients.consumer.ConsumerRecord

const val PERIODE_TOPIC = "paw.arbeidssokerperioder-v1"
const val HENDELSE_TOPIC = "paw.arbeidssoker-hendelseslogg-v1"

data class Record<A : Any>(
    val id: String,
    val value: A
)

fun AppContext.handleRecords(records: Iterable<ConsumerRecord<Long, ByteArray>>) {
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
    if (livenessHealthIndicator.getStatus() == HealthStatus.HEALTHY) {
        readinessHealthIndicator.setHealthy()
    }
}



class RecordsByType(source: Iterable<Record<Any>>)  {
    val map = source.groupBy { when (it.value) {
        is Periode -> Periode::class
        is Hendelse -> Hendelse::class
        else -> throw IllegalArgumentException("Unknown type: ${it.value}")
    } }
    @Suppress("UNCHECKED_CAST")
    inline fun <reified A: Any> get(): Iterable<Record<A>> {
        return (map[A::class]?.let { it as Iterable<Record<A>> }) ?: emptyList()
    }
}

fun AppContext.deserializeRecord(topic: String, bytes: ByteArray): Any? {
    return when (topic) {
        PERIODE_TOPIC -> periodeDeserializer.deserialize(topic, bytes)
        HENDELSE_TOPIC -> hendelseDeserializer.deserialize(topic, bytes)
        else -> {
            appLogger.warn("Ignoring record from from topic: $topic")
            null
        }
    }
}

fun AppContext.lagrePerioder(records: Iterable<Record<Periode>>) {
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

fun AppContext.lagreHendelser(records: Iterable<Record<Hendelse>>) {
    records.map { record ->
        Row(id = record.id, value = hendelseRad(encoder, record.value))
    }.takeIf { it.isNotEmpty() }
        ?.also { periodeRader ->
            bqDatabase.write(
                tableName = HENDELSE_TABELL,
                rows = periodeRader
            )
        }
}