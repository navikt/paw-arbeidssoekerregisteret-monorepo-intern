package no.nav.paw.arbeidssokerregisteret.app.config.helpers

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.time.Duration
import kotlin.reflect.KClass

class KafkaConsumerProperties<K, V>(
    val groupId: String,
    val keyDeSerializer: KClass<out Deserializer<K>>,
    val valueDeSerializer: KClass<out Deserializer<V>>,
    val autoOffsetReset: String = "earliest",
    val autoCommit: Boolean = false,
    val maxRecordsPrBatch: Int = 100,
    val maxPollInterval: Duration = Duration.ofMinutes(5)
): KafkaProperties {
    init {
        require(groupId.isNotBlank()) { "'groupId' kan ikke være tom(eller bare bestå av mellomrom)" }
        require(maxPollInterval.toMillis() < Int.MAX_VALUE.toLong()) { "'maxPollInterval' er for stor($maxPollInterval), maks grensen er ${Int.MAX_VALUE-1}ms"}
    }

    override val map: Map<String, Any>
        get() = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to autoCommit,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to maxRecordsPrBatch,
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to maxPollInterval.toMillis().toInt(),
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to keyDeSerializer.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to valueDeSerializer.java
        )
}