package no.nav.paw.bekreftelse.topology

import no.nav.paw.bekreftelse.config.ApplicationConfig
import no.nav.paw.bekreftelse.config.ApplicationIdSuffix
import no.nav.paw.bekreftelse.config.applicationIdSuffix
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

fun buildBekreftelseKafkaTopologyList(applicationConfig: ApplicationConfig): List<Pair<ApplicationIdSuffix, Topology>> =
    with(applicationConfig.kafkaTopology) {
        bekreftelseKlienter.flatMap { bekreftelseKlient ->
            listOf(
                buildKafkaTopology(bekreftelseKlient.bekreftelseSourceTopic, bekreftelseTargetTopic),
                buildKafkaTopology(bekreftelseKlient.paaVegneAvSourceTopic, bekreftelsePaaVegneAvTargetTopic)
            ).map { bekreftelseKlient.applicationIdSuffix() to it }
        }
    }

private fun buildKafkaTopology(
    sourceTopic: String,
    targetTopic: String
): Topology = StreamsBuilder().apply {
    stream<Long, Bekreftelse>(sourceTopic)
        .to(targetTopic)
}.build()
