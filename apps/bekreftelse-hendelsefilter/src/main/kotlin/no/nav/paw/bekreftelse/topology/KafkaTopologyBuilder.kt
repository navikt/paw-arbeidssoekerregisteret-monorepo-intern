package no.nav.paw.bekreftelse.topology

import no.nav.paw.bekreftelse.config.ApplicationConfig
import no.nav.paw.bekreftelse.config.ApplicationIdSuffix
import no.nav.paw.bekreftelse.config.bekreftelseApplicationIdSuffix
import no.nav.paw.bekreftelse.config.bekreftelsePaaVegneAvApplicationIdSuffix
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
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
                bekreftelseKlient.bekreftelseSourceTopic,
                applicationConfig.kafkaTopology.bekreftelseTargetTopic
            ),
            bekreftelseKlient.bekreftelsePaaVegneAvApplicationIdSuffix to buildKafkaTopology<PaaVegneAv>(
                bekreftelseKlient.paaVegneAvSourceTopic,
                applicationConfig.kafkaTopology.bekreftelsePaaVegneAvTargetTopic
            )
        )
    }

fun <T : SpecificRecord> buildKafkaTopology(
    sourceTopic: String,
    targetTopic: String
): Topology = StreamsBuilder().apply {
    stream<Long, T>(sourceTopic)
        .peek { _, _ -> logger.debug("Mottok melding på topic {}", sourceTopic) }
        .to(targetTopic)
}.build()
