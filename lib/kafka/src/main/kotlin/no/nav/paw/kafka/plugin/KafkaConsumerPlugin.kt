package no.nav.paw.kafka.plugin

import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import no.nav.paw.kafka.consumer.KafkaConsumerWrapper
import no.nav.paw.kafka.runner.KafkaConsumerAsyncRunner
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val PLUGIN_NAME_SUFFIX = "KafkaConsumerPlugin"

class KafkaConsumerPluginConfig<K, V> {
    var onInit: (() -> Unit)? = null
    var onConsume: ((ConsumerRecords<K, V>) -> Unit)? = null
    var kafkaConsumerWrapper: KafkaConsumerWrapper<K, V>? = null
    var executorService: ExecutorService = Executors.newSingleThreadExecutor()
    var keepRunning: AtomicBoolean = AtomicBoolean(true)
    var mayInterruptOnStop: AtomicBoolean = AtomicBoolean(false)
}

@Suppress("FunctionName")
fun <K, V> KafkaConsumerPlugin(pluginInstance: Any): ApplicationPlugin<KafkaConsumerPluginConfig<K, V>> {
    val pluginName = "${pluginInstance}${PLUGIN_NAME_SUFFIX}"
    return createApplicationPlugin(pluginName, ::KafkaConsumerPluginConfig) {
        application.log.info("Installerer {}", pluginName)
        val kafkaConsumerWrapper = requireNotNull(pluginConfig.kafkaConsumerWrapper) { "KafkaConsumerWrapper er null" }
        val onInit = pluginConfig.onInit ?: {}
        val onConsume = requireNotNull(pluginConfig.onConsume) { "Consume function er null" }
        val asyncRunner = KafkaConsumerAsyncRunner(
            onInit = onInit,
            onConsume = onConsume,
            kafkaConsumerWrapper = kafkaConsumerWrapper,
            executorService = pluginConfig.executorService,
            keepRunning = pluginConfig.keepRunning,
            mayInterruptOnStop = pluginConfig.mayInterruptOnStop
        )

        on(MonitoringEvent(ApplicationStarted)) {
            asyncRunner.init(pluginInstance)
            asyncRunner.start(pluginInstance)
        }

        on(MonitoringEvent(ApplicationStopping)) {
            asyncRunner.stop(pluginInstance)
        }
    }
}
