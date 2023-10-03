package no.nav.paw.arbeidssokerregisteret.app.config.helpers

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serializer
import java.time.Duration
import kotlin.reflect.KClass

class KafkaProducerProperties<K, V> (
    val producerId: String,
    val keySerializer: KClass<out Serializer<K>>,
    val valueSerializer: KClass<out Serializer<V>>,
    val ackConfig: String = "all",
    val linger: Duration = Duration.ofMillis(500)
): KafkaProperties {
    init {
        require(producerId.isNotEmpty()) { "'producerId' kan ikke være tom(eller bare bestå av mellomrom)" }
    }
    override val map: Map<String, Any>
        get() = mapOf(
            ProducerConfig.CLIENT_ID_CONFIG to producerId,
            ProducerConfig.ACKS_CONFIG to ackConfig,
            ProducerConfig.LINGER_MS_CONFIG to linger.toMillis(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to keySerializer.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializer.java
        )

}