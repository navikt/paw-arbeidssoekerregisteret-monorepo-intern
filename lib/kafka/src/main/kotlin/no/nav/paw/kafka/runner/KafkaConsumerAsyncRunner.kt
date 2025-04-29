package no.nav.paw.kafka.runner

import no.nav.paw.async.runner.ThreadPoolAsyncRunner
import no.nav.paw.kafka.consumer.KafkaConsumerWrapper
import no.nav.paw.logging.logger.buildNamedLogger
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumerAsyncRunner<K, V>(
    private val onInit: () -> Unit,
    private val onConsume: (ConsumerRecords<K, V>) -> Unit,
    private val kafkaConsumerWrapper: KafkaConsumerWrapper<K, V>,
    executorService: ExecutorService = Executors.newSingleThreadExecutor(),
    keepRunning: AtomicBoolean = AtomicBoolean(true),
    mayInterruptOnStop: AtomicBoolean = AtomicBoolean(false)
) : ThreadPoolAsyncRunner<ConsumerRecords<K, V>>(executorService, keepRunning, mayInterruptOnStop) {
    private val logger = buildNamedLogger("kafka.consumer")

    fun init(instance: Any) {
        logger.info("Klargjør Kafka Consumer {}", instance)
        onInit()
        kafkaConsumerWrapper.init()
    }

    fun start(instance: Any) {
        logger.info("Starter Kafka Consumer {}", instance)
        run(onRun = {
            kafkaConsumerWrapper.consume(onConsume)
        })
    }

    fun stop(instance: Any) {
        abort(onAbort = {
            logger.info("Stopper Kafka Consumer {}", instance)
            kafkaConsumerWrapper.stop()
        })
    }
}