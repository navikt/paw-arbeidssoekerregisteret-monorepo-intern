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
    var consumeFunction: ((ConsumerRecords<K, V>) -> Unit)? = null
    var successFunction: ((ConsumerRecords<K, V>) -> Unit)? = null
    var errorFunction: ((throwable: Throwable) -> Unit)? = null
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
        val consumeFunction = requireNotNull(pluginConfig.consumeFunction) { "ConsumeFunction er null" }
        val successFunction = pluginConfig.successFunction ?: kafkaConsumer::defaultSuccessFunction
        val errorFunction = pluginConfig.errorFunction ?: kafkaConsumer::defaultErrorFunction
        val asyncRunner = KafkaConsumerAsyncRunner(
            consumeFunction = consumeFunction,
            successFunction = successFunction,
            errorFunction = errorFunction,
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
