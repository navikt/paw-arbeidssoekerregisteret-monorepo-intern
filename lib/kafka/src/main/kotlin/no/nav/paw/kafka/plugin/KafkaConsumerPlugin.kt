package no.nav.paw.kafka.plugin

import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import no.nav.paw.kafka.consumer.defaultErrorFunction
import no.nav.paw.kafka.consumer.defaultSuccessFunction
import no.nav.paw.kafka.listener.NoopConsumerRebalanceListener
import no.nav.paw.kafka.runner.KafkaConsumerAsyncRunner
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val PLUGIN_NAME_SUFFIX = "KafkaConsumerPlugin"

class KafkaConsumerPluginConfig<K, V> {
    var onConsume: ((ConsumerRecords<K, V>) -> Unit)? = null
    var onSuccess: ((ConsumerRecords<K, V>) -> Unit)? = null
    var onFailure: ((throwable: Throwable) -> Unit)? = null
    var kafkaConsumer: KafkaConsumer<K, V>? = null
    var kafkaTopics: Collection<String>? = null
    var pollTimeout: Duration = Duration.ofMillis(100)
    var closeTimeout: Duration = Duration.ofMillis(500)
    var rebalanceListener: ConsumerRebalanceListener = NoopConsumerRebalanceListener()
    var executorService: ExecutorService = Executors.newSingleThreadExecutor()
}

@Suppress("FunctionName")
fun <K, V> KafkaConsumerPlugin(pluginInstance: Any): ApplicationPlugin<KafkaConsumerPluginConfig<K, V>> {
    val pluginName = "${pluginInstance}${PLUGIN_NAME_SUFFIX}"
    return createApplicationPlugin(pluginName, ::KafkaConsumerPluginConfig) {
        application.log.info("Installerer {}", pluginName)
        val kafkaTopics = requireNotNull(pluginConfig.kafkaTopics) { "KafkaTopics er null" }
        val kafkaConsumer = requireNotNull(pluginConfig.kafkaConsumer) { "KafkaConsumer er null" }
        val onConsume = requireNotNull(pluginConfig.onConsume) { "Consume function er null" }
        val onSuccess = pluginConfig.onSuccess ?: kafkaConsumer::defaultSuccessFunction
        val onFailure = pluginConfig.onFailure ?: kafkaConsumer::defaultErrorFunction
        val asyncRunner = KafkaConsumerAsyncRunner(
            onConsume = onConsume,
            onSuccess = onSuccess,
            onFailure = onFailure,
            kafkaConsumer = kafkaConsumer,
            kafkaTopics = kafkaTopics,
            pollTimeout = pluginConfig.pollTimeout,
            closeTimeout = pluginConfig.closeTimeout,
            rebalanceListener = pluginConfig.rebalanceListener,
            executorService = pluginConfig.executorService
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
