package no.nav.paw.kafka.consumer

import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.kafka.handler.NoopConsumerExceptionHandler
import no.nav.paw.kafka.listener.NoopConsumerRebalanceListener
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class NonCommittingKafkaConsumerWrapper<K, V>(
    private val topics: Collection<String>,
    private val consumer: Consumer<K, V>,
    private val pollTimeout: Duration = Duration.ofMillis(100),
    private val exceptionHandler: ConsumerExceptionHandler = NoopConsumerExceptionHandler(),
    private val rebalanceListener: ConsumerRebalanceListener = NoopConsumerRebalanceListener(),
) : KafkaConsumerWrapper<K, V> {
    private val logger = buildLogger
    private val isRunning = AtomicBoolean(false)
    override fun init() {
        logger.info("Kafka Consumer abonnerer på topics {}", topics)
        consumer.subscribe(topics, rebalanceListener)
    }

    override fun consume(onConsume: (ConsumerRecords<K, V>) -> Unit) {
        try {
            val records = consumer.poll(pollTimeout)
            isRunning.set(true)
            onConsume(records)
        } catch (throwable: Throwable) {
            isRunning.set(false)
            exceptionHandler.handleException(throwable)
        }
    }

    override fun stop() {
        logger.info("Kafka Consumer stopper å abonnere på topics {} og lukkes", topics)
        consumer.unsubscribe()
        consumer.close()
        isRunning.set(false)
    }

    fun isRunning(): Boolean {
        return isRunning.get()
    }
}