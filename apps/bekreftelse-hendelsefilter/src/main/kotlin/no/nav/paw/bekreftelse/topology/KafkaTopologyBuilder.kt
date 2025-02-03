package no.nav.paw.bekreftelse.topology

import no.nav.paw.bekreftelse.config.ApplicationConfig
import no.nav.paw.bekreftelse.config.ApplicationIdSuffix
import no.nav.paw.bekreftelse.config.applicationIdSuffix
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

fun buildBekreftelseKafkaTopologyList(applicationConfig: ApplicationConfig): List<Pair<ApplicationIdSuffix, Topology>> =
    applicationConfig.bekreftelseKlienter.flatMap { bekreftelseKlient ->
        listOf(
            ApplicationIdSuffix("bekreftelse_${bekreftelseKlient.applicationIdSuffix()}") to buildKafkaTopology(
                bekreftelseKlient.bekreftelseSourceTopic,
                applicationConfig.kafkaTopology.bekreftelseTargetTopic
            ),
            ApplicationIdSuffix("paaVegneAv_${bekreftelseKlient.applicationIdSuffix()}") to buildKafkaTopology(
                bekreftelseKlient.paaVegneAvSourceTopic,
                applicationConfig.kafkaTopology.bekreftelsePaaVegneAvTargetTopic
            )
        )
    }

private fun buildKafkaTopology(
    sourceTopic: String,
    targetTopic: String
): Topology = StreamsBuilder().apply {
    stream<Long, Bekreftelse>(sourceTopic)
        .to(targetTopic)
}.build()
