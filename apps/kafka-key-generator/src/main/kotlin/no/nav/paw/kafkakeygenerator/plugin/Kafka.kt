package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.kafkakeygenerator.config.KafkaTopologyConfig
import no.nav.paw.kafkakeygenerator.service.KafkaConsumerService
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.installKafkaPlugins(
    kafkaTopologyConfig: KafkaTopologyConfig,
    kafkaConsumer: KafkaConsumer<Long, Hendelse>,
    kafkaConsumerService: KafkaConsumerService
) {

    install(KafkaConsumerPlugin<Long, Hendelse>("Hendelselogg")) {
        this.onConsume = kafkaConsumerService::handleRecords
        this.onFailure = kafkaConsumerService::handleException
        this.kafkaConsumer = kafkaConsumer
        this.kafkaTopics = listOf(kafkaTopologyConfig.hendelseloggTopic)
    }
}
