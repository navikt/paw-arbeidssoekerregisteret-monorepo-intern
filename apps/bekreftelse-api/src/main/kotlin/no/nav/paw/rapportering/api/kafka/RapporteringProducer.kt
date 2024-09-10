package no.nav.paw.rapportering.api.kafka

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.config.kafka.sendDeferred
import no.nav.paw.rapportering.api.ApplicationInfo
import no.nav.paw.rapportering.api.config.ApplicationConfig
import no.nav.paw.rapportering.api.domain.request.RapporteringRequest
import no.nav.paw.rapportering.api.utils.logger
import no.nav.paw.rapportering.internehendelser.RapporteringTilgjengelig
import no.nav.paw.rapportering.melding.v1.Melding
import no.nav.paw.rapportering.melding.v1.vo.Bruker
import no.nav.paw.rapportering.melding.v1.vo.Metadata
import no.nav.paw.rapportering.melding.v1.vo.Svar
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import java.time.Instant

class RapporteringProducer(
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
        val topic = applicationConfig.rapporteringTopic
        val record = ProducerRecord(topic, key, message)
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: offset={}", recordMetadata.offset())
    }

    fun closeProducer() {
        producer.close()
    }
}

fun createMelding(state: RapporteringTilgjengelig, rapportering: RapporteringRequest): Melding = TODO()
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
//    .setVilFortsetteSomArbeidssoeker(rapportering.vilFortsetteSomArbeidssoeker)
//    .setHarJobbetIDennePerioden(rapportering.harJobbetIDennePerioden)
//    .build()
