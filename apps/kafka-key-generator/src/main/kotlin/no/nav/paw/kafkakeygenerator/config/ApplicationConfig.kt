package no.nav.paw.kafkakeygenerator.config

import java.time.Duration

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val pawHendelseConsumer: KafkaConsumerConfig,
    val pawPeriodeConsumer: KafkaConsumerConfig,
    val pdlAktorConsumer: KafkaConsumerConfig,
    val pawIdentitetProducer: KafkaProducerConfig,
    val identitetKonfliktJob: ScheduledJobConfig,
    val identitetHendelseJob: ScheduledJobConfig
)

data class KafkaConsumerConfig(
    val version: Int,
    val topic: String,
    val groupIdPrefix: String,
) {
    val groupId: String get() = "$groupIdPrefix-v$version"
    val clientId: String get() = "$groupIdPrefix-v$version-consumer"
}

data class KafkaProducerConfig(
    val version: Int,
    val topic: String,
    val clientIdPrefix: String
) {
    val clientId: String get() = "$clientIdPrefix-v$version-producer"
}

data class ScheduledJobConfig(
    val enabled: Boolean,
    val delay: Duration,
    val interval: Duration,
    val batchSize: Int
)