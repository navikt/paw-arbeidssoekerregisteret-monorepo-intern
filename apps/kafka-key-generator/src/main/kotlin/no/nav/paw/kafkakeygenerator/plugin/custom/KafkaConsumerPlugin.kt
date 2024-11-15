package no.nav.paw.kafkakeygenerator.plugin.custom

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.util.KtorDsl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.paw.config.kafka.asSequence
import no.nav.paw.kafkakeygenerator.listener.NoopConsumerRebalanceListener
import no.nav.paw.kafkakeygenerator.utils.buildApplicationLogger
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

val KafkaConsumerReady: EventDefinition<Application> = EventDefinition()

@KtorDsl
class KafkaConsumerPluginConfig<K, V> {
    var consumeFunction: ((Sequence<ConsumerRecords<K, V>>) -> Unit)? = null
    var successFunction: ((Unit) -> Unit)? = null
    var errorFunction: ((throwable: Throwable) -> Unit)? = null
    var kafkaConsumer: KafkaConsumer<K, V>? = null
    var kafkaTopics: Collection<String>? = null
    var pollTimeout: Duration? = null
    var closeTimeout: Duration? = null
    var rebalanceListener: ConsumerRebalanceListener? = null
    val shutdownFlag = AtomicBoolean(false)

    companion object {
        const val PLUGIN_NAME = "KafkaConsumerPlugin"
    }
}

fun <K, V> kafkaConsumerPlugin(): ApplicationPlugin<KafkaConsumerPluginConfig<K, V>> =
    createApplicationPlugin(KafkaConsumerPluginConfig.PLUGIN_NAME, ::KafkaConsumerPluginConfig) {
        application.log.info("Oppretter {}", KafkaConsumerPluginConfig.PLUGIN_NAME)
        val logger = buildApplicationLogger
        val consumeFunction = requireNotNull(pluginConfig.consumeFunction) { "ConsumeFunction er null" }
        val successFunction = pluginConfig.successFunction ?: { logger.debug("Kafka Consumer poll fullførte") }
        val errorFunction = pluginConfig.errorFunction ?: { logger.error("Kafka Consumer poll feilet") }
        val kafkaConsumer = requireNotNull(pluginConfig.kafkaConsumer) { "KafkaConsumer er null" }
        val kafkaTopics = requireNotNull(pluginConfig.kafkaTopics) { "KafkaTopics er null" }
        val pollTimeout = pluginConfig.pollTimeout ?: Duration.ofMillis(100)
        val closeTimeout = pluginConfig.closeTimeout ?: Duration.ofSeconds(1)
        val rebalanceListener = pluginConfig.rebalanceListener ?: NoopConsumerRebalanceListener()
        val shutdownFlag = pluginConfig.shutdownFlag
        var consumeJob: Job? = null

        on(MonitoringEvent(ApplicationStarted)) { application ->
            logger.info("Kafka Consumer klargjøres")
            kafkaConsumer.subscribe(kafkaTopics, rebalanceListener)
            application.environment.monitor.raise(KafkaConsumerReady, application)
        }

        on(MonitoringEvent(ApplicationStopping)) { _ ->
            logger.info("Kafka Consumer stopper")
            kafkaConsumer.unsubscribe()
            kafkaConsumer.close(closeTimeout)
            shutdownFlag.set(true)
            consumeJob?.cancel()
        }

        on(MonitoringEvent(KafkaConsumerReady)) { application ->
            consumeJob = application.launch(Dispatchers.IO) {
                logger.info("Kafka Consumer starter")
                kafkaConsumer
                    .asSequence(
                        stop = shutdownFlag,
                        pollTimeout = pollTimeout,
                        closeTimeout = closeTimeout
                    )
                    .runCatching(consumeFunction)
                    .mapCatching { kafkaConsumer.commitSync() }
                    .fold(onSuccess = successFunction, onFailure = errorFunction)
                logger.info("Kafka Consumer avsluttet")
            }
        }
    }
