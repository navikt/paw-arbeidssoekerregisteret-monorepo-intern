package no.nav.paw.bekreftelse.api.kafka

import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.utils.buildMeldingSerde
import no.nav.paw.bekreftelse.api.utils.logger
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.config.kafka.sendDeferred
import no.nav.paw.rapportering.melding.v1.Melding
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer

class BekreftelseProducer(
    private val applicationConfig: ApplicationConfig,
) {
    private lateinit var producer: Producer<Long, Melding>
    private val meldingSerde = buildMeldingSerde()

    init {
        initializeProducer()
    }

    private fun initializeProducer() {
        val kafkaFactory = KafkaFactory(applicationConfig.kafkaClients)
        producer =
            kafkaFactory.createProducer<Long, Melding>(
                clientId = applicationConfig.kafkaTopology.producerId,
                keySerializer = LongSerializer::class,
                valueSerializer = meldingSerde.serializer()::class
            )
    }

    suspend fun produceMessage(key: Long, message: Melding) {
        val topic = applicationConfig.kafkaTopology.bekreftelseTopic
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
