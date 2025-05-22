package no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse

import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseTable.recordKey
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface HendelseRecordRepository {
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
    ): List<StoredHendelseRecord>

    fun readAllRecordsForId(
        consumerVersion: Int,
        hendelseDeserializer: HendelseDeserializer,
        arbeidssoekerId: Long,
        merged: Boolean,
    ): List<StoredHendelseRecord>

    fun hentIdentitetsnummerForPeriodeId(
        hendelseDeserializer: HendelseDeserializer,
        periodeId: UUID,
    ): String?
}

object HendelseRecordPostgresRepository : HendelseRecordRepository {
    override fun <A : Hendelse> writeRecord(
        consumerVersion: Int,
        hendelseSerializer: HendelseSerializer,
        record: ConsumerRecord<Long, A>,
    ) {
        transaction {
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
    }

    override fun hentIdentitetsnummerForPeriodeId(
        hendelseDeserializer: HendelseDeserializer,
        periodeId: UUID,
    ): String? = transaction {
        TransactionManager.current()
            .exec(
                stmt = """select data from hendelser where data @> '{"hendelseId": "$periodeId"}' and data @> '{"hendelseType": "intern.v1.startet"}' limit 1;""",
                transform = { rs ->
                    if (rs.next()) {
                        hendelseDeserializer
                            .deserializeFromString(rs.getString(HendelseTable.data.name))
                            .identitetsnummer
                    } else null
                }
            )
    }

    override fun readAllNestedRecordsForId(
        consumerVersion: Int,
        hendelseDeserializer: HendelseDeserializer,
        arbeidssoekerId: Long,
        merged: Boolean,
    ): List<StoredHendelseRecord> {
        return transaction {
            val tmp = readAllRecordsForId(
                hendelseDeserializer = hendelseDeserializer,
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
                        hendelseDeserializer = hendelseDeserializer,
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
        hendelseDeserializer: HendelseDeserializer,
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
                        data = hendelseDeserializer.deserializeFromString(it[HendelseTable.data]),
                        merged = merged
                    )
                }
        }
}
