package no.nav.paw.kafkakeygenerator.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val pawHendelseConsumer: KafkaConsumerConfig,
    val pawPeriodeConsumer: KafkaConsumerConfig,
    val pdlAktorConsumer: KafkaConsumerConfig
)

data class KafkaConsumerConfig(
    val version: Int,
    val topic: String,
    val defaultPartitionCount: Int,
    val groupIdPrefix: String,
) {
    val groupId: String get() = "$groupIdPrefix-v$version"
    val clientId: String get() = "$groupIdPrefix-v$version-consumer"
}