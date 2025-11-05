package no.nav.paw.kafkakeygenerator.config

import no.nav.paw.kafka.config.KafkaConsumerConfig
import no.nav.paw.kafka.config.KafkaProducerConfig
import java.time.Duration

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val pawPeriodeConsumer: KafkaConsumerConfig,
    val pdlAktorConsumer: KafkaConsumerConfig,
    val pawIdentitetProducer: KafkaProducerConfig,
    val pawHendelseloggProducer: KafkaProducerConfig,
    val identitetMergeKonfliktJob: ScheduledJobConfig,
    val identitetSplittKonfliktJob: ScheduledJobConfig
)

data class ScheduledJobConfig(
    val enabled: Boolean,
    val delay: Duration,
    val interval: Duration,
    val batchSize: Int
)