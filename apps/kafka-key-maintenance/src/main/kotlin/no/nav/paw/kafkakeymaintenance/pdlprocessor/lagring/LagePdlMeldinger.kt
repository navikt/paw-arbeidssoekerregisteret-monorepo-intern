package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.time.Instant

private val lagreAktorLogger = LoggerFactory.getLogger("lagreAktorMelding")
val lagreAktorMelding: TransactionContext.(ConsumerRecord<String, ByteArray>) -> Unit = { record ->
    if (record.value() == null || record.value().isEmpty()) {
        lagreAktorLogger.info(
            "Sletter aktør: null={}, empty={}",
            record.value() == null, record.value()?.isEmpty()
        )
        delete(record.key())
    } else {
        kotlin.runCatching {
            insertOrUpdate(
                record.key(),
                timestamp = Instant.ofEpochMilli(record.timestamp()),
                traceparant = record.headers().lastHeader("traceparent")?.value(),
                data = record.value()
            )
        }
    }
}
