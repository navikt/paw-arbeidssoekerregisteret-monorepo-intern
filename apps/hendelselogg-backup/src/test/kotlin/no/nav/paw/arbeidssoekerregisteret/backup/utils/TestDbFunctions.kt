package no.nav.paw.arbeidssoekerregisteret.backup.utils

import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseTable
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseTable.recordKey
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.StoredHendelseRecord
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val hendelseDeserializer = HendelseDeserializer()

fun readRecord(consumerVersion: Int, partition: Int, offset: Long): StoredHendelseRecord? = transaction {
    HendelseTable
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
                data = it[HendelseTable.data],
                merged = false
            )
        }
}

fun getOneRecordForId(id: String): StoredHendelseRecord? = transaction {
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
}
