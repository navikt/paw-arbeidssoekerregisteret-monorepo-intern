package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Instant

val lagreAktorMelding: TransactionContext.(ConsumerRecord<String, ByteArray>) -> Unit = { record ->
    insertOrUpdate(
        record.key(),
        timestamp = Instant.ofEpochMilli(record.timestamp()),
        traceparant = record.headers().lastHeader("traceparent")?.value(),
        data = record.value()
    )
}