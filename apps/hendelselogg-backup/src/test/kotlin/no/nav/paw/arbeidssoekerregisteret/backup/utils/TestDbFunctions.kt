package no.nav.paw.arbeidssoekerregisteret.backup.utils

import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseTable
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseTable.recordKey
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.StoredHendelseRecord
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager

private val hendelseDeserializer = HendelseDeserializer()

fun readRecord(consumerVersion: Int, partition: Int, offset: Long): StoredHendelseRecord? {
    return HendelseTable
        .selectAll()
        .where {
            (HendelseTable.partition eq partition) and
                    (HendelseTable.offset eq offset) and
                    (HendelseTable.version eq consumerVersion)
        }.singleOrNull()
        ?.let {
            StoredHendelseRecord(
                partition = it[HendelseTable.partition],
                offset = it[HendelseTable.offset],
                recordKey = it[recordKey],
                arbeidssoekerId = it[HendelseTable.arbeidssoekerId],
                traceparent = it[HendelseTable.traceparent],
                data = hendelseDeserializer.deserializeFromString(it[HendelseTable.data]),
                merged = false
            )
        }
}

fun getOneRecordForId(id: String): StoredHendelseRecord? =
    TransactionManager.current()
        .exec(
            stmt = """select * from hendelser where data @> '{"identitetsnummer": "$id"}' limit 1;""",
            transform = { rs ->
                sequence {
                    while (rs.next()) {
                        yield(
                            StoredHendelseRecord(
                                partition = rs.getInt(HendelseTable.partition.name),
                                offset = rs.getLong(HendelseTable.offset.name),
                                recordKey = rs.getLong(recordKey.name),
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