package no.nav.paw.arbeidssokerregisteret.app.config

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig

data class KafkaKonfigurasjon(
    val streamKonfigurasjon: StreamKonfigurasjon,
    val serverKonfigurasjon: KafkaServerKonfigurasjon,
    val schemaRegistryKonfigurasjon: SchemaRegistryKonfigurasjon
) {

    val properties = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to streamKonfigurasjon.applikasjonsId,
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to serverKonfigurasjon.kafkaBrokers,
        StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.Long().javaClass.name,
        StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to SpecificAvroSerde::class.java.name,
        KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryKonfigurasjon.url,
        KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to schemaRegistryKonfigurasjon.autoRegistrerSchema,
        KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
        KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY to RecordNameStrategy::class.java.name,
    ) + (if (serverKonfigurasjon.autentisering.equals("SSL", true)) {
        mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to serverKonfigurasjon.keystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to serverKonfigurasjon.credstorePassword,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to serverKonfigurasjon.truststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to serverKonfigurasjon.credstorePassword
        )
    } else emptyMap())
}
