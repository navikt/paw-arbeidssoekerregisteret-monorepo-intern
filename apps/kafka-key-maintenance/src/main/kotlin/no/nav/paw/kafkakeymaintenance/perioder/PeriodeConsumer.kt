package no.nav.paw.kafkakeymaintenance.perioder

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafkakeymaintenance.kafka.HwmRunnerProcessor
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import org.apache.kafka.clients.consumer.ConsumerRecord

class LagrePeriode: HwmRunnerProcessor<Long, Periode> {
    override fun process(txContext: TransactionContext, record: ConsumerRecord<Long, Periode>) {
        txContext.insertOrUpdate(periodeRad(record.value()))
    }

    override fun ignore(record: ConsumerRecord<Long, Periode>): Boolean = false
}
