package no.nav.paw.bqadapter

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bqadapter.bigquery.AppContext
import no.nav.paw.bqadapter.bigquery.BEKREFTELSE_HENDELSE_TABELL
import no.nav.paw.bqadapter.bigquery.BEKRFTELSE_TABELL
import no.nav.paw.bqadapter.bigquery.HENDELSE_TABELL
import no.nav.paw.bqadapter.bigquery.PAAVNEGEAV_TABELL
import no.nav.paw.bqadapter.bigquery.PERIODE_TABELL
import no.nav.paw.bqadapter.bigquery.Row
import no.nav.paw.bqadapter.bigquery.schema.bekreftelseHendelseRad
import no.nav.paw.bqadapter.bigquery.schema.bekreftelseRad
import no.nav.paw.bqadapter.bigquery.schema.hendelseRad
import no.nav.paw.bqadapter.bigquery.schema.periodeRad
import no.nav.paw.bqadapter.bigquery.schema.påVegneAvRad
import no.nav.paw.health.model.HealthStatus
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.checkerframework.checker.units.qual.A
import java.time.Instant

data class Record<A : Any>(
    val id: String,
    val recordTimestamp: Instant,
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
                    recordTimestamp = Instant.ofEpochMilli(record.timestamp()),
                    value = deserialized
                )
            }
    }.let(::RecordsByType)
    lagrePerioder(records.get())
    lagreHendelser(records.get())
    lagreBekreftelser(records.get())
    lagrePaaVegneAv(records.get())
    lagreBekreftelserHendelser(records.get())
    if (livenessHealthIndicator.getStatus() == HealthStatus.HEALTHY) {
        readinessHealthIndicator.setHealthy()
    }
}



class RecordsByType(source: Iterable<Record<Any>>)  {
    val map = source.groupBy { when (it.value) {
        is Periode -> Periode::class
        is Hendelse -> Hendelse::class
        is Bekreftelse -> Bekreftelse::class
        is PaaVegneAv -> PaaVegneAv::class
        is BekreftelseHendelse -> BekreftelseHendelse::class
        else -> throw IllegalArgumentException("Unknown type: ${it.value}")
    } }
    @Suppress("UNCHECKED_CAST")
    inline fun <reified A: Any> get(): Iterable<Record<A>> {
        return (map[A::class]?.let { it as Iterable<Record<A>> }) ?: emptyList()
    }
}

fun AppContext.deserializeRecord(topic: String, bytes: ByteArray): Any? {
    return when (topic) {
        topics.periodeTopic -> deserializers.periodeDeserializer.deserialize(topic, bytes)
        topics.hendelseloggTopic -> deserializers.hendelseDeserializer.deserialize(topic, bytes)
        topics.bekreftelseTopic -> deserializers.bekreftelseDeserializer.deserialize(topic, bytes)
        topics.paavnegneavTopic -> deserializers.påVegneAvDeserializers.deserialize(topic, bytes)
        topics.bekreftelseHendelseloggTopic -> deserializers.bekreftelseHendelseDeserializer.deserialize(topic, bytes)
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

fun AppContext.lagreBekreftelser(records: Iterable<Record<Bekreftelse>>) {
    records.map { record ->
        Row(id = record.id, value = bekreftelseRad(encoder, record.value))
    }.takeIf { it.isNotEmpty() }
        ?.also { bekreftelseRader ->
            bqDatabase.write(
                tableName = BEKRFTELSE_TABELL,
                rows = bekreftelseRader
            )
    }
}

fun AppContext.lagreBekreftelserHendelser(records: Iterable<Record<BekreftelseHendelse>>) {
    records.map { record ->
        Row(id = record.id, value = bekreftelseHendelseRad(encoder, record.value))
    }.takeIf { it.isNotEmpty() }
        ?.also { bekreftelseRader ->
            bqDatabase.write(
                tableName = BEKREFTELSE_HENDELSE_TABELL,
                rows = bekreftelseRader
            )
        }
}

fun AppContext.lagrePaaVegneAv(records: Iterable<Record<PaaVegneAv>>) {
    records.mapIndexed { index, record ->
        Row(
            id = record.id,
            value = påVegneAvRad(
                encoder = encoder,
                recordTimestamp = record.recordTimestamp,
                paaVegneAv = record.value,
                batchOrder = index.toLong()
            )
        )
    }.takeIf { it.isNotEmpty() }
        ?.also { paaVegneAvRader ->
            bqDatabase.write(
                tableName = PAAVNEGEAV_TABELL,
                rows = paaVegneAvRader
            )
    }
}