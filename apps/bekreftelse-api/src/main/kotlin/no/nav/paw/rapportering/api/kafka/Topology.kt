package no.nav.paw.rapportering.api.kafka

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelseSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed

fun StreamsBuilder.appTopology(
    prometheusRegistry: PrometheusMeterRegistry,
    rapporteringHendelseLoggTopic: String,
    rapporteringStateStoreName: String,
): Topology {
    stream(rapporteringHendelseLoggTopic, Consumed.with(Serdes.Long(), RapporteringsHendelseSerde()))
        .oppdaterRapporteringHendelseState(rapporteringStateStoreName)

    return build()
}