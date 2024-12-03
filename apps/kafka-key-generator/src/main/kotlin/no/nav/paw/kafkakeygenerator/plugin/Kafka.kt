package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.kafkakeygenerator.plugin.custom.kafkaConsumerPlugin
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer

fun <K, V> Application.configureKafka(
    consumeFunction: ((ConsumerRecords<K, V>) -> Unit),
    successFunction: ((ConsumerRecords<K, V>) -> Unit)? = null,
    errorFunction: ((throwable: Throwable) -> Unit),
    kafkaConsumer: KafkaConsumer<K, V>,
    kafkaTopics: List<String>
) {

    install(kafkaConsumerPlugin<K, V>()) {
        this.consumeFunction = consumeFunction
        this.successFunction = successFunction
        this.errorFunction = errorFunction
        this.kafkaConsumer = kafkaConsumer
        this.kafkaTopics = kafkaTopics
    }
}
