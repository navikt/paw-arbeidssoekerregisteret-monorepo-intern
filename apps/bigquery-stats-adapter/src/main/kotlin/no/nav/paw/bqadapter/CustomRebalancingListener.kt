package no.nav.paw.bqadapter

import no.nav.paw.arbeidssokerregisteret.TopicNames
import no.nav.paw.bqadapter.bigquery.BigqueryDatabase
import no.nav.paw.bqadapter.bigquery.PAAVNEGEAV_TABELL
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

class CustomRebalancingListener<K, V>(
    private val bqDatabase: BigqueryDatabase,
    private val topicNames: TopicNames,
    private val consumer: Consumer<K, V>,
) : ConsumerRebalanceListener {
    private val logger = buildLogger

    override fun onPartitionsRevoked(p0: Collection<TopicPartition?>?) {}

    override fun onPartitionsAssigned(p0: Collection<TopicPartition?>?) {
        if (bqDatabase.isEmpty(PAAVNEGEAV_TABELL)) {
            logger.info("BigQuery-tabellen {} er tom", PAAVNEGEAV_TABELL)
            val topicPartitions = p0?.filter { it?.topic() == topicNames.paavnegneavTopic }
            if (!topicPartitions.isNullOrEmpty()) {
                consumer.seekToBeginning(topicPartitions)
                logger.info("Seek to beginning: ${topicPartitions.map { "${it?.topic()}:${it?.partition()}" }}")
            } else {
                logger.info("Ingen partisjoner for topic ${topicNames.paavnegneavTopic} tildelt ved rebalance")
            }
        } else {
            logger.info("BigQuery-tabellen {} er ikke tom", PAAVNEGEAV_TABELL)
        }
    }
}