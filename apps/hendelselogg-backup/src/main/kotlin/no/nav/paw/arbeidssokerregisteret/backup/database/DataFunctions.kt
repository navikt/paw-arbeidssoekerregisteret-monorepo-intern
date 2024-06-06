package no.nav.paw.arbeidssokerregisteret.backup.database

import no.nav.paw.arbeidssokerregisteret.backup.database.HendelseTable.recordKey
import no.nav.paw.arbeidssokerregisteret.backup.vo.StoredData
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

context(HendelseSerializer)
fun <A: Hendelse> Transaction.writeRecord(record: ConsumerRecord<Long, A>) {
    HendelseTable.insert {
        it[partition] = record.partition()
        it[offset] = record.offset()
        it[recordKey] = record.key()
        it[arbeidssoekerId] = record.value().id
        it[data] = serializeToString(record.value())
    }
}

context(HendelseDeserializer)
fun Transaction.readRecord(partition: Int, offset: Long): StoredData? =
    HendelseTable
        .selectAll()
        .where { (HendelseTable.partition eq partition) and (HendelseTable.offset eq offset) }
        .singleOrNull()
        ?.let {
            StoredData(
                partition = it[HendelseTable.partition],
                offset = it[HendelseTable.offset],
                recordKey = it[recordKey],
                arbeidssoekerId = it[HendelseTable.arbeidssoekerId],
                data = deserializeFromString(it[HendelseTable.data])
            )
        }

