package no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse

import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseTable.recordKey
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.contains
import java.util.*

interface HendelseRecordRepository {
    fun <A : Hendelse> writeRecord(
        consumerVersion: Int,
        record: ConsumerRecord<Long, A>,
    )

    fun readAllNestedRecordsForId(
        consumerVersion: Int,
        arbeidssoekerId: Long,
        merged: Boolean,
    ): List<StoredHendelseRecord>

    fun readAllRecordsForId(
        consumerVersion: Int,
        arbeidssoekerId: Long,
        merged: Boolean,
    ): List<StoredHendelseRecord>

    fun hentIdentitetsnummerForPeriodeId(
        periodeId: UUID,
    ): String?
}

object HendelseRecordPostgresRepository : HendelseRecordRepository {
    override fun <A : Hendelse> writeRecord(
        consumerVersion: Int,
        record: ConsumerRecord<Long, A>,
    ) {
        transaction {
            HendelseTable.insert {
                it[version] = consumerVersion
                it[partition] = record.partition()
                it[offset] = record.offset()
                it[recordKey] = record.key()
                it[arbeidssoekerId] = record.value().id
                it[data] = record.value()
                it[traceparent] = record.headers().lastHeader("traceparent")?.let { h -> String(h.value()) }
            }
        }
    }

    override fun hentIdentitetsnummerForPeriodeId(
        periodeId: UUID,
    ): String? = transaction {
        val hasHendelseType = HendelseTable.data.contains("{\"hendelseType\": \"intern.v1.startet\"}")
        val hasHendelseId = HendelseTable.data.contains("{\"hendelseId\": \"$periodeId\"}")
        HendelseTable.selectAll()
            .where { hasHendelseType and hasHendelseId }
            .limit(count = 1)
            .singleOrNull()?.let { it[HendelseTable.data] }?.identitetsnummer
    }

    override fun readAllNestedRecordsForId(
        consumerVersion: Int,
        arbeidssoekerId: Long,
        merged: Boolean,
    ): List<StoredHendelseRecord> {
        return transaction {
            val tmp = readAllRecordsForId(
                arbeidssoekerId = arbeidssoekerId,
                merged = merged,
                consumerVersion = consumerVersion
            )
            tmp.asSequence()
                .map(StoredHendelseRecord::data)
                .filterIsInstance<ArbeidssoekerIdFlettetInn>()
                .map { it.kilde.arbeidssoekerId }
                .distinct()
                .flatMap {
                    readAllNestedRecordsForId(
                        arbeidssoekerId = it,
                        merged = true,
                        consumerVersion = consumerVersion
                    )
                }.toList()
                .plus(tmp)
                .sortedBy { it.data.metadata.tidspunkt }
        }
    }

    override fun readAllRecordsForId(
        consumerVersion: Int,
        arbeidssoekerId: Long,
        merged: Boolean,
    ): List<StoredHendelseRecord> =
        transaction {
            HendelseTable
                .selectAll()
                .where {
                    (HendelseTable.arbeidssoekerId eq arbeidssoekerId) and
                            (HendelseTable.version eq consumerVersion)
                }
                .map {
                    StoredHendelseRecord(
                        partition = it[HendelseTable.partition],
                        offset = it[HendelseTable.offset],
                        recordKey = it[recordKey],
                        arbeidssoekerId = it[HendelseTable.arbeidssoekerId],
                        traceparent = it[HendelseTable.traceparent],
                        data = it[HendelseTable.data],
                        merged = merged
                    )
                }
        }
}
