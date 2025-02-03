package no.nav.paw.bekreftelse.topology

import no.nav.paw.bekreftelse.config.ApplicationConfig
import no.nav.paw.bekreftelse.config.ApplicationIdSuffix
import no.nav.paw.bekreftelse.config.bekreftelseApplicationIdSuffix
import no.nav.paw.bekreftelse.config.bekreftelsePaaVegneAvApplicationIdSuffix
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.processor.mapRecord
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.utils.buildApplicationLogger
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

private val logger = buildApplicationLogger

fun buildKafkaTopologyList(applicationConfig: ApplicationConfig): List<Pair<ApplicationIdSuffix, Topology>> =
    applicationConfig.bekreftelseKlienter.flatMap { bekreftelseKlient ->
        listOf(
            bekreftelseKlient.bekreftelseApplicationIdSuffix to buildKafkaTopology<Bekreftelse>(
                streamId = bekreftelseKlient.applicationIdSuffix,
                sourceTopic = bekreftelseKlient.bekreftelseSourceTopic,
                targetTopic = applicationConfig.kafkaTopology.bekreftelseTargetTopic
            ),
            bekreftelseKlient.bekreftelsePaaVegneAvApplicationIdSuffix to buildKafkaTopology<PaaVegneAv>(
                streamId = bekreftelseKlient.applicationIdSuffix,
                sourceTopic = bekreftelseKlient.paaVegneAvSourceTopic,
                targetTopic = applicationConfig.kafkaTopology.bekreftelsePaaVegneAvTargetTopic
            )
        )
    }

fun <T : SpecificRecord> buildKafkaTopology(
    streamId: String,
    sourceTopic: String,
    targetTopic: String
): Topology = StreamsBuilder().apply {
    stream<Long, T>(sourceTopic)
        .peek { _, _ -> logger.debug("Mottok melding på topic {}", sourceTopic) }
    stream<Long, Bekreftelse>(sourceTopic)
        .mapRecord(name = "add_source_header") { record ->
            val headers = record.headers()
            val updatedHeaders = headers.add("source", streamId.toByteArray())
            record.withHeaders(updatedHeaders)
        }
        .to(targetTopic)
}.build()
