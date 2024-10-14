package no.nav.paw.bekreftelse.api.plugins.custom

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
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

val KafkaConsumerReady: EventDefinition<Application> = EventDefinition()

@KtorDsl
class KafkaConsumerPluginConfig {
    var consumeFunction: ((ConsumerRecord<Long, BekreftelseHendelse>) -> Unit)? = null
    var errorFunction: ((throwable: Throwable) -> Unit)? = null
    var consumer: KafkaConsumer<Long, BekreftelseHendelse>? = null
    var topic: String? = null
    var pollTimeout: Duration = Duration.ofMillis(100)
    var shutDownTimeout: Duration = Duration.ofMillis(500)
    var rebalanceListener: ConsumerRebalanceListener? = null
    val pollingFlag = AtomicBoolean(true)

    companion object {
        const val PLUGIN_NAME = "KafkaConsumerPlugin"
    }
}

val KafkaConsumerPlugin: ApplicationPlugin<KafkaConsumerPluginConfig> =
    createApplicationPlugin(KafkaConsumerPluginConfig.PLUGIN_NAME, ::KafkaConsumerPluginConfig) {
        application.log.info("Oppretter {}", KafkaConsumerPluginConfig.PLUGIN_NAME)
        val consumeFunction = requireNotNull(pluginConfig.consumeFunction) { "ConsumeFunction er null" }
        val errorFunction = pluginConfig.errorFunction ?: { }
        val consumer = requireNotNull(pluginConfig.consumer) { "KafkaConsumes er null" }
        val topic = requireNotNull(pluginConfig.topic) { "Topic er null" }
        val pollTimeout = requireNotNull(pluginConfig.pollTimeout) { "PollTimeout er null" }
        val shutDownTimeout = requireNotNull(pluginConfig.shutDownTimeout) { "ShutDownTimeout er null" }
        val rebalanceListener = pluginConfig.rebalanceListener
        val pollingFlag = pluginConfig.pollingFlag
        var consumeJob: Job? = null

        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.info("Starter Kafka Consumer")
            if (rebalanceListener == null) {
                consumer.subscribe(listOf(topic))
            } else {
                consumer.subscribe(listOf(topic), rebalanceListener)
            }
            application.environment.monitor.raise(KafkaConsumerReady, application)
        }

        on(MonitoringEvent(ApplicationStopping)) { application ->
            application.log.info("Stopper Kafka Consumer")
            pollingFlag.set(false)
            consumeJob?.cancel()
            consumer.close(shutDownTimeout)
        }

        on(MonitoringEvent(KafkaConsumerReady)) { application ->
            consumeJob = application.launch(Dispatchers.IO) {
                try {
                    application.log.info("Starter Kafka Consumer polling")
                    while (pollingFlag.get()) {
                        application.log.trace("Polling Kafka Consumer for records")
                        val records = consumer.poll(pollTimeout)
                        records.forEach { consumeFunction(it) }
                        consumer.commitSync()
                    }
                    application.log.info("Kafka Consumer polling avsluttet")
                } catch (e: Exception) {
                    application.log.error("Kafka Consumer polling avbrutt med feil", e)
                    errorFunction(e)
                }
            }
        }
    }
