package no.nav.paw.config.kafka.streams


import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import java.time.Duration
import java.time.Instant

fun <K, V> KStream<K, V>.filterWithContext(
    name: String,
    vararg stateStoreNames: String,
    function: ProcessorContext<K, V>.(V) -> Boolean
): KStream<K, V> {
    val processor = {
        GenericProcessor<K, V, K, V> { record ->
            if (function(record.value())) forward(record)
        }
    }
    return process(processor, Named.`as`(name), *stateStoreNames)
}

fun <K, V_IN, V_OUT> KStream<K, V_IN>.mapNonNull(
    name: String,
    vararg stateStoreNames: String,
    function: ProcessorContext<K, V_OUT>.(V_IN) -> V_OUT?
): KStream<K, V_OUT> {
    val processor = {
        GenericProcessor<K, V_IN, K, V_OUT> { record ->
            val result = function(record.value())
            if (result != null) forward(record.withValue(result))
        }
    }
    return process(processor, Named.`as`(name), *stateStoreNames)
}

fun <K, V_IN, V_OUT> KStream<K, V_IN>.mapWithContext(
    name: String,
    vararg stateStoreNames: String,
    function: ProcessorContext<K, V_OUT>.(V_IN) -> V_OUT
): KStream<K, V_OUT> {
    val processor = {
        GenericProcessor<K, V_IN, K, V_OUT> { record ->
            val result = function(record.value())
            forward(record.withValue(result))
        }
    }
    return process(processor, Named.`as`(name), *stateStoreNames)
}

fun <K_IN, V_IN, K_OUT, V_OUT> KStream<K_IN, V_IN>.mapKeyAndValue(
    name: String,
    vararg stateStoreNames: String,
    function: ProcessorContext<K_OUT, V_OUT>.(K_IN, V_IN) -> Pair<K_OUT, V_OUT>?
): KStream<K_OUT, V_OUT> {
    val processor = {
        GenericProcessor<K_IN, V_IN, K_OUT, V_OUT> { record ->
            val result = function(record.key(), record.value())
            if (result != null) {
                forward(record.withKey(result.first).withValue(result.second))
            }
        }
    }
    return process(processor, Named.`as`(name), *stateStoreNames)
}

fun <K_IN, V_IN, K_OUT, V_OUT> KStream<K_IN, V_IN>.genericProcess(
    name: String,
    vararg stateStoreNames: String,
    punctuation: Punctuation<K_OUT, V_OUT>? = null,
    function: ProcessorContext<K_OUT, V_OUT>.(Record<K_IN, V_IN>) -> Unit
): KStream<K_OUT, V_OUT> {
    val processor = {
        GenericProcessor(function = function, punctuation = punctuation)
    }
    return process(processor, Named.`as`(name), *stateStoreNames)
}

class GenericProcessor<K_IN, V_IN, K_OUT, V_OUT>(
    private val punctuation: Punctuation<K_OUT, V_OUT>? = null,
    private val function: ProcessorContext<K_OUT, V_OUT>.(Record<K_IN, V_IN>) -> Unit
) : Processor<K_IN, V_IN, K_OUT, V_OUT> {
    private var context: ProcessorContext<K_OUT, V_OUT>? = null

    override fun init(context: ProcessorContext<K_OUT, V_OUT>?) {
        super.init(context)
        this.context = context
        if (punctuation != null) {
            context?.schedule(punctuation.interval, punctuation.type) { time ->
                punctuation.function(Instant.ofEpochMilli(time), context)
            }
        }
    }

    override fun process(record: Record<K_IN, V_IN>?) {
        if (record == null) return
        val ctx = requireNotNull(context) { "Context is not initialized" }
        with(ctx) { function(record) }
    }
}

data class Punctuation<K, V>(
    val interval: Duration,
    val type: PunctuationType,
    val function: (Instant, ProcessorContext<K, V>) -> Unit
)
