package no.nav.paw.bekreftelse.topology

import no.nav.paw.bekreftelse.config.ApplicationConfig
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

fun buildBekreftelseKafkaTopologyList(applicationConfig: ApplicationConfig): List<Topology> =
    with(applicationConfig.kafkaTopology) {
        listOf(
            buildKafkaTopology(teamDagpengerBekreftelseSourceTopic, bekreftelseTargetTopic)
        )
    }

fun buildBekreftelsePaaVegneAvKafkaTopologyList(applicationConfig: ApplicationConfig): List<Topology> =
    with(applicationConfig.kafkaTopology) {
        listOf(
            buildKafkaTopology(teamDagpengerBekreftelsePaaVegneAvSourceTopic, bekreftelseTargetTopic)
        )
    }

private fun buildKafkaTopology(
    sourceTopic: String,
    targetTopic: String
): Topology = StreamsBuilder().apply {
    stream<Long, Bekreftelse>(sourceTopic)
        .to(targetTopic)
}.build()
