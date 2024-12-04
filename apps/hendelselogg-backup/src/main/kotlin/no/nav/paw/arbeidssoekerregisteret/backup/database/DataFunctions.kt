package no.nav.paw.arbeidssoekerregisteret.backup.database

import no.nav.paw.arbeidssoekerregisteret.backup.database.HendelseTable.recordKey
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.vo.StoredData
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun <A : Hendelse> TransactionContext.writeRecord(hendelseSerializer: HendelseSerializer, record: ConsumerRecord<Long, A>) {
    HendelseTable.insert {
        it[version] = appContext.consumerVersion
        it[partition] = record.partition()
        it[offset] = record.offset()
        it[recordKey] = record.key()
        it[arbeidssoekerId] = record.value().id
        it[data] = hendelseSerializer.serializeToString(record.value())
        it[traceparent] = record.headers().lastHeader("traceparent")?.let { h -> String(h.value()) }
    }
}

fun TransactionContext.readRecord(hendelseDeserializer: HendelseDeserializer, partition: Int, offset: Long): StoredData? =
    HendelseTable
        .selectAll()
        .where {
            (HendelseTable.partition eq partition) and
                    (HendelseTable.offset eq offset) and
                    (HendelseTable.version eq appContext.consumerVersion)
        }.singleOrNull()
        ?.let {
            StoredData(
                partition = it[HendelseTable.partition],
                offset = it[HendelseTable.offset],
                recordKey = it[recordKey],
                arbeidssoekerId = it[HendelseTable.arbeidssoekerId],
                traceparent = it[HendelseTable.traceparent],
                data = hendelseDeserializer.deserializeFromString(it[HendelseTable.data]),
                merged = false
            )
        }

fun TransactionContext.readAllNestedRecordsForId(
    hendelseDeserializer: HendelseDeserializer,
    arbeidssoekerId: Long,
    merged: Boolean = false
): List<StoredData> {
    val tmp = readAllRecordsForId(hendelseDeserializer = hendelseDeserializer, arbeidssoekerId = arbeidssoekerId, merged = merged)
    return tmp.asSequence()
        .map(StoredData::data)
        .filterIsInstance<ArbeidssoekerIdFlettetInn>()
        .map { it.kilde.arbeidssoekerId }
        .distinct()
        .flatMap { readAllNestedRecordsForId(hendelseDeserializer = hendelseDeserializer, arbeidssoekerId = it, merged = true) }.toList()
        .plus(tmp)
        .sortedBy { it.data.metadata.tidspunkt }
}

fun TransactionContext.readAllRecordsForId(
    hendelseDeserializer: HendelseDeserializer,
    arbeidssoekerId: Long,
    merged: Boolean = false
): List<StoredData> =
    HendelseTable
        .selectAll()
        .where {
            (HendelseTable.arbeidssoekerId eq arbeidssoekerId) and
                    (HendelseTable.version eq appContext.consumerVersion)
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

fun Transaction.getOneRecordForId(hendelseDeserializer: HendelseDeserializer, id: String): StoredData? =
    TransactionManager.current()
        .exec(
            stmt = """select * from hendelser where data @> '{"identitetsnummer": "$id"}' limit 1;""",
            transform = { rs ->
                sequence {
                    while (rs.next()) {
                        yield(
                            StoredData(
                                partition = rs.getInt(HendelseTable.partition.name),
                                offset = rs.getLong(HendelseTable.offset.name),
                                recordKey = rs.getLong(HendelseTable.recordKey.name),
                                arbeidssoekerId = rs.getLong(HendelseTable.arbeidssoekerId.name),
                                data = hendelseDeserializer.deserializeFromString(rs.getString(HendelseTable.data.name)),
                                traceparent = rs.getString(HendelseTable.traceparent.name),
                                merged = false
                            )
                        )
                    }
                }.toList().firstOrNull()
            }
        )