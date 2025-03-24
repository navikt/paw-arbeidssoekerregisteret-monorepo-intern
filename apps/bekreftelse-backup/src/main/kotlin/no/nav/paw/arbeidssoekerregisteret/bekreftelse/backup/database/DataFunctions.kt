package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database

import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.insert

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

