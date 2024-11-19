package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import io.opentelemetry.api.trace.Span
import no.nav.paw.kafkakeymaintenance.kafka.HwmRunnerProcessor
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.Instant

private val lagreAktorLogger = LoggerFactory.getLogger("lagreAktorMelding")
class LagreAktorMelding: HwmRunnerProcessor<String, ByteArray> {
    override fun process(txContext: TransactionContext, record: ConsumerRecord<String, ByteArray>) {
        if (record.value() == null || record.value().isEmpty()) {
            lagreAktorLogger.info(
                "Sletter aktør: null={}, empty={}",
                record.value() == null, record.value()?.isEmpty()
            )
            txContext.delete(record.key())
        } else {
            val traceparent = Span.current().spanContext.let { ctx ->
                "00-${ctx.traceId}-${ctx.spanId}-${ctx.traceFlags.asHex()}"
            }
            kotlin.runCatching {
                txContext.insertOrUpdate(
                    record.key(),
                    timestamp = Instant.now(),
                    traceparent = traceparent.toByteArray(),
                    data = record.value()
                )
            }
        }
    }
}
