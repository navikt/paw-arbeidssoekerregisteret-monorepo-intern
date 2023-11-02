package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serializer
import java.time.Duration
import kotlin.reflect.KClass

fun <K, V> kafkaProducerProperties(
    producerId: String,
    keySerializer: KClass<out Serializer<K>>,
    valueSerializer: KClass<out Serializer<V>>,
    ackConfig: String = "all",
    linger: Duration = Duration.ZERO
): Map<String, Any> {
    require(producerId.isNotEmpty()) { "'producerId' kan ikke være tom(eller bare bestå av mellomrom)" }
    return mapOf(
        ProducerConfig.CLIENT_ID_CONFIG to producerId,
        ProducerConfig.ACKS_CONFIG to ackConfig,
        ProducerConfig.LINGER_MS_CONFIG to linger.toMillis(),
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to keySerializer.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializer.java,
        KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY to "io.confluent.kafka.serializers.subject.RecordNameStrategy"
    )
}