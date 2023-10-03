package no.nav.paw.arbeidssokerregisteret.app.config.helpers

import no.nav.paw.arbeidssokerregisteret.app.config.nais.KAFKA_CREDSTORE_PASSWORD
import no.nav.paw.arbeidssokerregisteret.app.config.nais.KAFKA_KEYSTORE_PATH
import no.nav.paw.arbeidssokerregisteret.app.config.nais.KAFKA_TRUSTSTORE_PATH
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs

sealed interface KafkaAuthentication: KafkaProperties {

    object Unsecure: KafkaAuthentication {
        override val map: Map<String, Any>  = emptyMap()
    }

    data class Aiven(
        val keyStorePath: String,
        val credStorePassword: String,
        val truststorePath: String
    ): KafkaAuthentication {
        override val map: Map<String, String>
            get() = mapOf(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
                SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to keyStorePath,
                SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to credStorePassword,
                SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to truststorePath,
                SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to credStorePassword
            )
    }

    fun Map<String, String>.Aiven(): Aiven = Aiven(
        keyStorePath = konfigVerdi(KAFKA_KEYSTORE_PATH),
        credStorePassword = konfigVerdi(KAFKA_CREDSTORE_PASSWORD),
        truststorePath = konfigVerdi(KAFKA_TRUSTSTORE_PATH)
    )

}