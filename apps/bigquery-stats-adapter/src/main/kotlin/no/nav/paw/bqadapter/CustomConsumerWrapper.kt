package no.nav.paw.bqadapter

import no.nav.paw.arbeidssokerregisteret.TopicNames
import no.nav.paw.bqadapter.bigquery.BigqueryDatabase
import no.nav.paw.bqadapter.bigquery.PAAVNEGEAV_TABELL
import no.nav.paw.bqadapter.bigquery.TableName
import no.nav.paw.kafka.consumer.KafkaConsumerWrapper
import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.kafka.handler.NoopConsumerExceptionHandler
import no.nav.paw.kafka.listener.NoopConsumerRebalanceListener
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.time.Duration

class CustomConsumerWrapper<K, V>(
    private val bqDatabase: BigqueryDatabase,
    private val topicNames: TopicNames,
    private val topics: Collection<String>,
    private val consumer: Consumer<K, V>,
    private val pollTimeout: Duration = Duration.ofMillis(100),
    private val exceptionHandler: ConsumerExceptionHandler = NoopConsumerExceptionHandler(),
    private val rebalanceListener: ConsumerRebalanceListener = NoopConsumerRebalanceListener()
) : KafkaConsumerWrapper<K, V> {
    private val logger = buildLogger

    override fun init() {
        logger.info("Kafka Consumer abonnerer på topics {}", topics)
        consumer.subscribe(topics, rebalanceListener)
        if (bqDatabase.isEmpty(PAAVNEGEAV_TABELL)) {
            logger.info("BigQuery-tabellen {} er tom ved oppstart av consumer", PAAVNEGEAV_TABELL)
            val topicPartitions = (0 until 5).map { TopicPartition(topicNames.paavnegneavTopic, it) }
            consumer.seekToBeginning(topicPartitions)
            logger.info("Seek to begning: ${topicPartitions.map { "${it.topic()}:${it.partition()}" }}")
        } else {
            logger.info("BigQuery-tabellen {} er ikke tom ved oppstart av consumer", PAAVNEGEAV_TABELL)
        }
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
        consumer.close()
    }
}