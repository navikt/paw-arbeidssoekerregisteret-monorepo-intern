package no.nav.paw.kafka.runner

import no.nav.paw.async.runner.ThreadPoolAsyncRunner
import no.nav.paw.kafka.listener.NoopConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class KafkaConsumerAsyncRunner<K, V>(
    private val consumeFunction: (ConsumerRecords<K, V>) -> Unit,
    private val successFunction: (ConsumerRecords<K, V>) -> Unit,
    private val errorFunction: (throwable: Throwable) -> Unit,
    private val kafkaConsumer: KafkaConsumer<K, V>,
    private val kafkaTopics: Collection<String>,
    private val pollTimeout: Duration = Duration.ofMillis(100),
    private val closeTimeout: Duration = Duration.ofMillis(500),
    private val rebalanceListener: ConsumerRebalanceListener = NoopConsumerRebalanceListener(),
    executorService: ExecutorService = Executors.newSingleThreadExecutor()
) : ThreadPoolAsyncRunner<ConsumerRecords<K, V>>(executorService = executorService, recursive = true) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private fun consumeTask(): ConsumerRecords<K, V> {
        val records = kafkaConsumer.poll(pollTimeout)
        consumeFunction(records)
        return records
    }

    fun init(instance: Any) {
        logger.info("Klargjør Kafka Consumer {}", instance)
        kafkaConsumer.subscribe(kafkaTopics, rebalanceListener)
    }

    fun start(instance: Any) {
        logger.info("Starter Kafka Consumer {}", instance)
        run(
            task = ::consumeTask,
            onSuccess = successFunction,
            onFailure = errorFunction
        )
    }

    fun stop(instance: Any) {
        abort(onAbort = {
            logger.info("Stopper Kafka Consumer {}", instance)
            kafkaConsumer.unsubscribe()
            kafkaConsumer.close(closeTimeout)
        })
    }
}