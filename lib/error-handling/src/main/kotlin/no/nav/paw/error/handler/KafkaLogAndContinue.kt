package no.nav.paw.error.handler
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafka.factory.PROMETHEUS_METER_REGISTRY
import no.nav.paw.tracing.ClosableSpan
import no.nav.paw.tracing.initSpan
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.streams.errors.DeserializationExceptionHandler
import org.apache.kafka.streams.errors.DeserializationExceptionHandler.DeserializationHandlerResponse
import org.apache.kafka.streams.processor.ProcessorContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KafkaLogAndContinueExceptionHandler : DeserializationExceptionHandler {
    var metrics: PrometheusMeterRegistry? = null
    val log: Logger = LoggerFactory.getLogger(KafkaLogAndContinueExceptionHandler::class.java)

    override fun handle(
        context: ProcessorContext,
        record: ConsumerRecord<ByteArray, ByteArray>,
        exception: Exception
    ): DeserializationHandlerResponse {
        val span = record.traceparent()?.let { traceparent ->
            initSpan(
                traceparent = traceparent,
                instrumentationScopeName = this::class.simpleName!!,
                spanName = "deserialization_error"
            )
        } ?: ClosableSpan(null)
        return span.use {
            metrics?.counter(
                "kafka_deserialization_error",
                Tags.of(
                    Tag.of("topic", record.topic()),
                    Tag.of("partition", record.partition().toString())
                )
            )?.count()
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
                "invalid record.value: could not be deserialized by ${
                    context.valueSerde().deserializer()::class.simpleName
                }, message size: ${record.value()?.size} bytes"
            }
            Span.current().setAllAttributes(
                Attributes.of(
                    AttributeKey.stringKey("kafka.topic"), record.topic(),
                    AttributeKey.longKey("kafka.partition"), record.partition().toLong(),
                    AttributeKey.longKey("kafka.offset"), record.offset(),
                )
            )
            Span.current().setStatus(StatusCode.ERROR, message)
            DeserializationHandlerResponse.CONTINUE
        }
    }

    override fun configure(configs: Map<String?, *>?) {
        metrics = configs?.get(PROMETHEUS_METER_REGISTRY) as? PrometheusMeterRegistry
        log.info("Configured with metrics: $metrics")
    }
}

fun ConsumerRecord<*, *>.traceparent(): String? = headers()
    .lastHeader("traceparent")
    ?.value()
    ?.toString(Charsets.UTF_8)
    ?.takeIf { it.isNotBlank() }
