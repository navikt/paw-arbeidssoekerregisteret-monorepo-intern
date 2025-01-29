package no.nav.paw.error.handler
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.streams.errors.DeserializationExceptionHandler
import org.apache.kafka.streams.errors.DeserializationExceptionHandler.DeserializationHandlerResponse
import org.apache.kafka.streams.processor.ProcessorContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class KafkaLogAndContinueExceptionHandler : DeserializationExceptionHandler {
    @WithSpan(
        value = "deserialization_error",
        kind = SpanKind.INTERNAL
    )
    override fun handle(
        context: ProcessorContext,
        record: ConsumerRecord<ByteArray, ByteArray>,
        exception: Exception
    ): DeserializationHandlerResponse {
        log.warn(
            "Exception caught during Deserialization, " +
                    "taskId: {}, topic: {}, partition: {}, offset: {}",
            context.taskId(), record.topic(), record.partition(), record.offset(),
            exception
        )
        val keyDeserializer = context.keySerde().deserializer()
        val key = runCatching {
            keyDeserializer.deserialize(record.topic(), record.headers(), record.key())
        }.getOrNull()
        val message = if (key == null) {
            "invalid record.key: could not be deserialized by ${keyDeserializer::class.simpleName}, key size: ${record.key()?.size} bytes"
        } else {
            "invalid record.value: could not be deserialized by ${context.valueSerde().deserializer()::class.simpleName}, message size: ${record.value()?.size} bytes"
        }
        Span.current().setStatus(StatusCode.ERROR, message)
        return DeserializationHandlerResponse.CONTINUE
    }

    override fun configure(configs: Map<String?, *>?) {
        // ignore
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(KafkaLogAndContinueExceptionHandler::class.java)
    }
}
