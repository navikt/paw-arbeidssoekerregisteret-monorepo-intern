package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.time.Instant

private val lagreAktorLogger = LoggerFactory.getLogger("lagreAktorMelding")
val lagreAktorMelding: TransactionContext.(ConsumerRecord<String, ByteArray>) -> Unit = { record ->
    when {
        record.value() == null || record.value().isEmpty() -> {
            lagreAktorLogger.info("Sletter aktør: null={}, empty={}", record.value() == null, record.value()?.isEmpty())
            delete(record.key())
        }

        else -> {
            if (record.key().isNullOrBlank()) {
                lagreAktorLogger.warn("Mottok melding med tom eller null key")
            }
            kotlin.runCatching {
                insertOrUpdate(
                    record.key().replace("\"", ""),
                    timestamp = Instant.ofEpochMilli(record.timestamp()),
                    traceparant = record.headers().lastHeader("traceparent")?.value(),
                    data = record.value()
                )
            }.onFailure { e ->
                lagreAktorLogger.error("Feil ved lagring av aktør: {}", record.key(), e)
            }.getOrThrow()
        }
    }
}
