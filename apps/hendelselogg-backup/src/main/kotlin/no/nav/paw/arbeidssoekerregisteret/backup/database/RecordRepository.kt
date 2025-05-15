package no.nav.paw.arbeidssoekerregisteret.backup.database

import no.nav.paw.arbeidssoekerregisteret.backup.vo.StoredData
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecord

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