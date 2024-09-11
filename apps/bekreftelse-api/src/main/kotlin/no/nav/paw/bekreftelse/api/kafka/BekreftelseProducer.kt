package no.nav.paw.bekreftelse.api.kafka

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.domain.BekreftelseRequest
import no.nav.paw.bekreftelse.api.utils.logger
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.config.kafka.sendDeferred
import no.nav.paw.rapportering.melding.v1.Melding
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

class BekreftelseProducer(
    private val kafkaConfig: KafkaConfig,
    private val applicationConfig: ApplicationConfig,
) {
    private lateinit var producer: Producer<Long, Melding>
    private val meldingSerde = SpecificAvroSerde<Melding>().apply {
        configure(mapOf("schema.registry.url" to kafkaConfig.schemaRegistry), false)
    }

    init {
        initializeProducer()
    }

    private fun initializeProducer() {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        producer =
            kafkaFactory.createProducer<Long, Melding>(
                clientId = applicationConfig.producerId,
                keySerializer = LongSerializer::class,
                valueSerializer = meldingSerde.serializer()::class
            )
    }

    suspend fun produceMessage(key: Long, message: Melding) {
        val topic = applicationConfig.bekreftelseTopic
        val record = ProducerRecord(topic, key, message)
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: offset={}", recordMetadata.offset())
    }

    fun closeProducer() {
        producer.close()
    }
}

fun createMelding(state: BekreftelseTilgjengelig, bekreftelse: BekreftelseRequest): Melding = TODO()
//Melding.newBuilder()
//    .setId(ApplicationInfo.id)
//    .setNamespace("paw")
//    .setPeriodeId(state.periodeId)
//    .setSvar(Svar(
//        Metadata(
//            Instant.now(),
//            Bruker
//        )
//    ))
//    .setGjelderFra(state.gjelderFra)
//    .setGjelderTil(state.gjelderTil)
//    .setVilFortsetteSomArbeidssoeker(bekreftelse.vilFortsetteSomArbeidssoeker)
//    .setHarJobbetIDennePerioden(bekreftelse.harJobbetIDennePerioden)
//    .build()
