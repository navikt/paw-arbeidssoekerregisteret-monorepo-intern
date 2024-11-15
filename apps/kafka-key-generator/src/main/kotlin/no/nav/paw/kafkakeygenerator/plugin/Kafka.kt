package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.kafkakeygenerator.config.KAFKA_TOPOLOGY_CONFIG
import no.nav.paw.kafkakeygenerator.config.KafkaTopologyConfig
import no.nav.paw.kafkakeygenerator.plugin.custom.kafkaConsumerPlugin
import no.nav.paw.kafkakeygenerator.service.KafkaConsumerService
import org.apache.kafka.common.serialization.LongDeserializer

fun Application.configureKafka(
    kafkaConsumerService: KafkaConsumerService
) {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
    val kafkaTopologyConfig = loadNaisOrLocalConfiguration<KafkaTopologyConfig>(KAFKA_TOPOLOGY_CONFIG)
    val kafkaFactory = KafkaFactory(kafkaConfig)

    val hendelseKafkaConsumer = kafkaFactory.createConsumer(
        groupId = kafkaTopologyConfig.consumerGroupId,
        clientId = "${kafkaTopologyConfig.consumerGroupId}-consumer",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = HendelseDeserializer::class
    )

    install(kafkaConsumerPlugin<Long, Hendelse>()) {
        consumeFunction = kafkaConsumerService::handleRecords
        errorFunction = kafkaConsumerService::handleException
        kafkaConsumer = hendelseKafkaConsumer
        kafkaTopics = listOf(kafkaTopologyConfig.hendelseloggTopic)
    }
}
