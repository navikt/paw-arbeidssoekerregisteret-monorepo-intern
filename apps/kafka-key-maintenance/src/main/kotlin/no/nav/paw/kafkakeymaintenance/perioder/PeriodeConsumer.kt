package no.nav.paw.kafkakeymaintenance.perioder

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import no.nav.paw.kafkakeymaintenance.kafka.topic
import no.nav.paw.kafkakeymaintenance.kafka.updateHwm
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

fun Sequence<Iterable<ConsumerRecord<Long, Periode>>>.consume(
    ctxFactory: Transaction.() -> TransactionContext
) {
    forEach { batch ->
        processBatch(ctxFactory, batch)
    }
}

@WithSpan(
    value = "process_batch_periode",
    kind = SpanKind.INTERNAL
)
private fun processBatch(
    ctxFactory: Transaction.() -> TransactionContext,
    batch: Iterable<ConsumerRecord<Long, Periode>>
) {
    transaction {
        val tx = ctxFactory()
        batch.forEach { periodeRecord ->
            val hwmValid = tx.updateHwm(
                topic = topic(periodeRecord.topic()),
                partition = periodeRecord.partition(),
                offset = periodeRecord.offset(),
                time = Instant.ofEpochMilli(periodeRecord.timestamp()),
                lastUpdated = Instant.now()
            )
            if (hwmValid) {
                val rad = periodeRad(periodeRecord.value())
                tx.insertOrUpdate(rad)
            } else {
                Span.current().addEvent("Below HWM")
            }
        }
    }
}