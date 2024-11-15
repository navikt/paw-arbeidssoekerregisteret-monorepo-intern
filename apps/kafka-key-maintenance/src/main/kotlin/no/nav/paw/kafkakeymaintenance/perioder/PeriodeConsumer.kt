package no.nav.paw.kafkakeymaintenance.perioder

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import org.apache.kafka.clients.consumer.ConsumerRecord

val lagrePeriode: TransactionContext.(ConsumerRecord<Long, Periode>) -> Unit = { record ->
    insertOrUpdate(periodeRad(record.value()))
}