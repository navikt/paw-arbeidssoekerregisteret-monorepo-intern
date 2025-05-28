package no.nav.paw.kafka.consumer

import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.kafka.handler.NoopConsumerExceptionHandler
import no.nav.paw.kafka.listener.NoopConsumerRebalanceListener
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Duration

class CommittingKafkaConsumerWrapper<K, V>(
    private val topics: Collection<String>,
    private val consumer: Consumer<K, V>,
    private val pollTimeout: Duration = Duration.ofMillis(100),
    private val closeTimeout: Duration = Duration.ofMillis(500),
    private val exceptionHandler: ConsumerExceptionHandler = NoopConsumerExceptionHandler(),
    private val rebalanceListener: ConsumerRebalanceListener = NoopConsumerRebalanceListener()
) : KafkaConsumerWrapper<K, V> {
    private val logger = buildLogger

    override fun init() {
        logger.info("Kafka Consumer abonnerer på topics {}", topics)
        consumer.subscribe(topics, rebalanceListener)
    }

    override fun consume(onConsume: (ConsumerRecords<K, V>) -> Unit) {
        try {
            val records = consumer.poll(pollTimeout)
            onConsume(records)
            if (!records.isEmpty) {
                consumer.commitSync()
            }
        } catch (throwable: Throwable) {
            exceptionHandler.handleException(throwable)
        }
    }

    override fun stop() {
        logger.info("Kafka Consumer stopper å abonnere på topics {} og lukkes", topics)
        consumer.unsubscribe()
        consumer.close(closeTimeout)
    }
}