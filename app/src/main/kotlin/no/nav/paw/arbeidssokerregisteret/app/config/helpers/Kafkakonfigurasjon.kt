package no.nav.paw.arbeidssokerregisteret.app.config.helpers

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.paw.arbeidssokerregisteret.app.config.helpers.KafkaAuthentication.Unsecure.Aiven
import no.nav.paw.arbeidssokerregisteret.app.config.nais.NaisEnv
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer

fun Map<String, String>.baseProperties(naisEnv: NaisEnv) =
    if (naisEnv == NaisEnv.Local) {
        KafkaBaseProperties(
            brokerUrl = konfigVerdi("KAFKA_BROKERS"),
            authentication = KafkaAuthentication.Unsecure
        )
    } else {
        KafkaBaseProperties(
            brokerUrl = konfigVerdi("KAFKA_BROKERS"),
            authentication = Aiven()
        )
    }

fun Map<String, String>.schemaRegistryProperties() =
    SchemaRegistryProperties(
        autoRegisterSchema = true,
        kafkaAvroSpecificReaderConfig = true
    )

fun avroProducerProperties(producerId: String) =
    KafkaProducerProperties(
        producerId = producerId,
        keySerializer = StringSerializer::class,
        valueSerializer = KafkaAvroSerializer::class
    )

fun avroConsumerProperties(groupId: String) =
    KafkaConsumerProperties(
        groupId = groupId,
        keyDeSerializer = StringDeserializer::class,
        valueDeSerializer = KafkaAvroDeserializer::class
    )

fun lagKafaProducerClient(
    config: KafkaProperties,
    vararg configs: KafkaProperties
): KafkaProducer<String, Any> =
    KafkaProducer(
        configs
            .map(KafkaProperties::map)
            .fold(config.map) { acc, map -> acc + map }
            .asProperties
    )

fun lagKafkaConsumerClient(
    config: KafkaProperties,
    vararg configs: KafkaProperties
): KafkaConsumer<String, Any> =
    KafkaConsumer(
        configs
            .map(KafkaProperties::map)
            .fold(config.map) { acc, map -> acc + map }
            .asProperties
    )
