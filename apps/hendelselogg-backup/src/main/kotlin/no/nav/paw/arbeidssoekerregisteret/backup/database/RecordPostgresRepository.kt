package no.nav.paw.arbeidssoekerregisteret.backup.database

import no.nav.paw.arbeidssoekerregisteret.backup.database.HendelseTable.recordKey
import no.nav.paw.arbeidssoekerregisteret.backup.vo.StoredData
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

interface RecordRepository {
    fun <A : Hendelse> writeRecord(
        consumerVersion: Int,
        hendelseSerializer: HendelseSerializer,
        record: ConsumerRecord<Long, A>,
    )

    fun readAllNestedRecordsForId(
        consumerVersion: Int,
        hendelseDeserializer: HendelseDeserializer,
        arbeidssoekerId: Long,
        merged: Boolean,
    ): List<StoredData>

    fun readAllRecordsForId(
        consumerVersion: Int,
        hendelseDeserializer: HendelseDeserializer,
        arbeidssoekerId: Long,
        merged: Boolean,
    ): List<StoredData>
}

object RecordPostgresRepository : RecordRepository {
    override fun <A : Hendelse> writeRecord(
        consumerVersion: Int,
        hendelseSerializer: HendelseSerializer,
        record: ConsumerRecord<Long, A>,
    ) {
        HendelseTable.insert {
            it[version] = consumerVersion
            it[partition] = record.partition()
            it[offset] = record.offset()
            it[recordKey] = record.key()
            it[arbeidssoekerId] = record.value().id
            it[data] = hendelseSerializer.serializeToString(record.value())
            it[traceparent] = record.headers().lastHeader("traceparent")?.let { h -> String(h.value()) }
        }
    }

    override fun readAllNestedRecordsForId(
        consumerVersion: Int,
        hendelseDeserializer: HendelseDeserializer,
        arbeidssoekerId: Long,
        merged: Boolean,
    ): List<StoredData> {
        val tmp = readAllRecordsForId(
            hendelseDeserializer = hendelseDeserializer,
            arbeidssoekerId = arbeidssoekerId,
            merged = merged,
            consumerVersion = consumerVersion
        )
        return tmp.asSequence()
            .map(StoredData::data)
            .filterIsInstance<ArbeidssoekerIdFlettetInn>()
            .map { it.kilde.arbeidssoekerId }
            .distinct()
            .flatMap {
                readAllNestedRecordsForId(
                    hendelseDeserializer = hendelseDeserializer,
                    arbeidssoekerId = it,
                    merged = true,
                    consumerVersion = consumerVersion
                )
            }.toList()
            .plus(tmp)
            .sortedBy { it.data.metadata.tidspunkt }
    }

    override fun readAllRecordsForId(
        consumerVersion: Int,
        hendelseDeserializer: HendelseDeserializer,
        arbeidssoekerId: Long,
        merged: Boolean,
    ): List<StoredData> =
        HendelseTable
            .selectAll()
            .where {
                (HendelseTable.arbeidssoekerId eq arbeidssoekerId) and
                        (HendelseTable.version eq consumerVersion)
            }
            .map {
                StoredData(
                    partition = it[HendelseTable.partition],
                    offset = it[HendelseTable.offset],
                    recordKey = it[recordKey],
                    arbeidssoekerId = it[HendelseTable.arbeidssoekerId],
                    traceparent = it[HendelseTable.traceparent],
                    data = hendelseDeserializer.deserializeFromString(it[HendelseTable.data]),
                    merged = merged
                )
            }
}
