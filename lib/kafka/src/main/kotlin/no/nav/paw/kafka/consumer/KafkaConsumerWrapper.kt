package no.nav.paw.kafka.consumer

import no.nav.paw.health.model.LivenessCheck
import no.nav.paw.health.model.ReadinessCheck
import org.apache.kafka.clients.consumer.ConsumerRecords

interface KafkaConsumerWrapper<K, V> : ReadinessCheck, LivenessCheck {

    fun init()

    fun consume(onConsume: (ConsumerRecords<K, V>) -> Unit)

    fun stop()
}