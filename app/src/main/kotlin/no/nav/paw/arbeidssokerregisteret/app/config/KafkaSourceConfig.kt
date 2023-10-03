package no.nav.paw.arbeidssokerregisteret.app.config

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.app.config.helpers.KafkaAuthentication
import no.nav.paw.arbeidssokerregisteret.app.config.helpers.KafkaAuthentication.Unsecure.Aiven
import no.nav.paw.arbeidssokerregisteret.app.config.helpers.konfigVerdi
import no.nav.paw.arbeidssokerregisteret.app.config.nais.KAFKA_BROKERS
import no.nav.paw.arbeidssokerregisteret.app.config.nais.NaisEnv
import no.nav.paw.arbeidssokerregisteret.app.config.nais.currentNaisEnv
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import java.util.*

class KafkaSourceConfig(map: Map<String, String>) {
    val kafkaStreamApplicationId: String = map.konfigVerdi("KAFKA_STREAM_APPLICATION_ID")

    val eventlogTopic = map.konfigVerdi<String>("EVENTLOG_TOPIC")

    private val standardProperties = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to kafkaStreamApplicationId,
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to map.konfigVerdi<String>(KAFKA_BROKERS),
        StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String().javaClass.name,
        StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to SpecificAvroSerde::class.java.name,
        "value.converter.schema.registry.url" to "http://localhost:8082",
        KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to true,
        KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true
    )

    val properties: Properties = Properties().apply {
        val autentisering = if (currentNaisEnv == NaisEnv.ProdGCP) {
            map.Aiven()
        } else {
            KafkaAuthentication.Unsecure
        }
        putAll(autentisering.map)
        putAll(standardProperties)
    }
}