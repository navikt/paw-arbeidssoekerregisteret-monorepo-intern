package no.nav.paw.arbeidssokerregisteret.app.config.helpers

import org.apache.kafka.clients.CommonClientConfigs
import java.util.Properties

interface KafkaProperties {
    val map: Map<String, Any>

}

val Map<String, Any>.asProperties: Properties get() = Properties(size).apply {
    this@asProperties.forEach { (key, value) -> put(key, value)}
}

class KafkaBaseProperties(
    val brokerUrl: String,
    val authentication: KafkaAuthentication,
    val additionalProperties: Map<String, Any> = emptyMap()
) : KafkaProperties {
    override val map: Map<String, Any>
        get() = authentication.map +
                additionalProperties +
                (CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to brokerUrl)
}

