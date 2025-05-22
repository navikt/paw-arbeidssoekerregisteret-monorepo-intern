package no.nav.paw.arbeidssoekerregisteret.backup.kafka

import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseRecordRepository
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.updateHwm
import no.nav.paw.arbeidssoekerregisteret.backup.metrics.Metrics
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.transactions.transaction

class HendelseloggBackup(
    private val hendelseRecordRepository: HendelseRecordRepository,
    private val metrics: Metrics,
) {
    fun processRecords(records: ConsumerRecords<Long, Hendelse>, consumerVersion: Int) = transaction {
        records.forEach { record ->
            if (updateHwm(consumerVersion, record.partition(), record.offset())) {
                hendelseRecordRepository.writeRecord(consumerVersion, HendelseSerializer(), record)
                metrics.recordCounter.increment()

                val hendelse = record.value()
                if (hendelse is Avsluttet) {
                    metrics.kalkulertAvsluttetAarsakCounters[hendelse.kalkulertAarsak]?.increment()
                }
            } else {
                metrics.duplicateRecordCounter.increment()
            }
        }
    }
}
