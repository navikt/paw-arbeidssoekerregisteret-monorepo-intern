package no.nav.paw.arbeidssoekerregisteret.backup.database

import no.nav.paw.arbeidssoekerregisteret.backup.database.HendelseTable.recordKey
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.vo.StoredData
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager

context(HendelseSerializer, ApplicationContext)
fun <A : Hendelse> Transaction.writeRecord(record: ConsumerRecord<Long, A>) {
    HendelseTable.insert {
        it[version] = consumerVersion
        it[partition] = record.partition()
        it[offset] = record.offset()
        it[recordKey] = record.key()
        it[arbeidssoekerId] = record.value().id
        it[data] = serializeToString(record.value())
        it[traceparent] = record.headers().lastHeader("traceparent")?.let { h -> String(h.value()) }
    }
}

context(HendelseDeserializer, ApplicationContext)
fun Transaction.readRecord(partition: Int, offset: Long): StoredData? =
    HendelseTable
        .selectAll()
        .where {
            (HendelseTable.partition eq partition) and
                    (HendelseTable.offset eq offset) and
                    (HendelseTable.version eq consumerVersion)
        }.singleOrNull()
        ?.let {
            StoredData(
                partition = it[HendelseTable.partition],
                offset = it[HendelseTable.offset],
                recordKey = it[recordKey],
                arbeidssoekerId = it[HendelseTable.arbeidssoekerId],
                traceparent = it[HendelseTable.traceparent],
                data = deserializeFromString(it[HendelseTable.data])
            )
        }

context(HendelseDeserializer, ApplicationContext)
fun Transaction.readAllRecordsForId(arbeidssoekerId: Long): List<StoredData> =
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
                data = deserializeFromString(it[HendelseTable.data])
            )
        }

context(HendelseDeserializer, ApplicationContext)
fun Transaction.getOneRecordForId(id: String): StoredData? =
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
                                data = deserializeFromString(rs.getString(HendelseTable.data.name)),
                                traceparent = rs.getString(HendelseTable.traceparent.name)
                            )
                        )
                    }
                }.toList().firstOrNull()
            }
        )