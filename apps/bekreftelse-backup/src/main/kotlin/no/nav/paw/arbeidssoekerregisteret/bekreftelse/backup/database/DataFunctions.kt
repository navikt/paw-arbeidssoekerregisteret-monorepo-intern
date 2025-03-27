package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.StoredData
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseDeserializer
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

fun TransactionContext.readAllRecordsForId(
    arbeidssoekerId: Long,
): List<StoredData> =
    BekreftelseHendelserTable
        .selectAll()
        .where {
            (BekreftelseHendelserTable.arbeidssoekerId eq arbeidssoekerId) and
                    (BekreftelseHendelserTable.version eq appContext.consumerVersion)
        }
        .map {
            StoredData(
                partition = it[BekreftelseHendelserTable.partition],
                offset = it[BekreftelseHendelserTable.offset],
                recordKey = it[BekreftelseHendelserTable.recordKey],
                arbeidssoekerId = it[BekreftelseHendelserTable.arbeidssoekerId],
                traceparent = it[BekreftelseHendelserTable.traceparent],
                data = BekreftelseHendelseDeserializer().deserializeFromString(it[BekreftelseHendelserTable.data]),
            )
        }

fun <A : BekreftelseHendelse> TransactionContext.writeHendelseRecord(hendelseSerializer: BekreftelseHendelseSerializer, record: ConsumerRecord<Long, A>) {
    BekreftelseHendelserTable.insert {
        it[version] = appContext.consumerVersion
        it[partition] = record.partition()
        it[offset] = record.offset()
        it[recordKey] = record.key()
        it[arbeidssoekerId] = record.value().arbeidssoekerId
        it[data] = hendelseSerializer.serializeToString(record.value())
        it[traceparent] = record.headers().lastHeader("traceparent")?.let { h -> String(h.value()) }
    }
}

fun TransactionContext.writeBekreftelseRecord(record: ConsumerRecord<Long, ByteArray>) =
    BekreftelserTable.insert {
        it[version] = appContext.consumerVersion
        it[partition] = record.partition()
        it[offset] = record.offset()
        it[recordKey] = record.key()
        it[data] = record.value()
        it[traceparent] = record.headers().lastHeader("traceparent")?.let { h -> String(h.value()) }
    }


fun TransactionContext.writePaaVegneAvRecord(record: ConsumerRecord<Long, ByteArray>) =
    PaaVegneAvTable.insert {
        it[version] = appContext.consumerVersion
        it[partition] = record.partition()
        it[offset] = record.offset()
        it[recordKey] = record.key()
        it[data] = record.value()
        it[traceparent] = record.headers().lastHeader("traceparent")?.let { h -> String(h.value()) }
    }

