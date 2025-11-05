package no.nav.paw.kafkakeygenerator.service

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.kafkakeygenerator.config.KafkaConsumerConfig
import no.nav.paw.kafkakeygenerator.model.dao.HwmTable
import no.nav.paw.kafkakeygenerator.model.dto.Hwm
import no.nav.paw.kafkakeygenerator.model.dto.asHwm
import no.nav.paw.kafkakeygenerator.utils.kafkaReceivedGauge
import no.nav.paw.logging.logger.buildNamedLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.MDC
import java.time.Instant

class KafkaHwmService(
    private val kafkaConsumerConfig: KafkaConsumerConfig,
    private val meterRegistry: MeterRegistry,
) : KafkaHwmOperations {
    private val logger = buildNamedLogger("application.kafka.hwm")

    override fun initHwm(
        topic: String,
        partitionCount: Int
    ): Int = transaction {
        with(kafkaConsumerConfig) {
            logger.info("Initializing HWM for {} partitions on topic {}", partitionCount, topic)
            (0 until partitionCount)
                .filter { HwmTable.getByTopicAndPartition(version, topic, it) == null }
                .sumOf { HwmTable.insert(version, topic, it, -1) }
        }
    }

    override fun getHwm(
        topic: String,
        partition: Int
    ): Hwm = transaction {
        with(kafkaConsumerConfig) {
            logger.info("Getting HWM for partition {} on topic {}", partition, topic)
            val hwmRow = HwmTable.getByTopicAndPartition(version, topic, partition)
            requireNotNull(hwmRow?.asHwm()) { "No HWM found for partition $partition on topic $topic, init not called?" }
        }
    }

    override fun updateHwm(
        topic: String,
        partition: Int,
        offset: Long,
        timestamp: Instant
    ): Int = transaction {
        with(kafkaConsumerConfig) {
            try {
                MDC.put("kafka_topic", topic)
                MDC.put("kafka_partition", "$partition")
                MDC.put("kafka_offset", "$offset")
                logger.debug("Updating HWM to offset {} for partition {} on topic {}", offset, partition, topic)
                meterRegistry.kafkaReceivedGauge(topic, partition, offset)
                HwmTable.update(version, topic, partition, offset, timestamp, Instant.now())
            } finally {
                MDC.remove("kafka_topic")
                MDC.remove("kafka_partition")
                MDC.remove("kafka_offset")
            }
        }
    }
}