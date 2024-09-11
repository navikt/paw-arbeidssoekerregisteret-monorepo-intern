package no.nav.paw.bekreftelse.api.kafka

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed

fun StreamsBuilder.appTopology(
    prometheusRegistry: PrometheusMeterRegistry,
    bekreftelseHendelseLoggTopic: String,
    bekreftelseStateStoreName: String,
): Topology {
    stream(bekreftelseHendelseLoggTopic, Consumed.with(Serdes.Long(), BekreftelseHendelseSerde()))
        .oppdaterBekreftelseHendelseState(bekreftelseStateStoreName)

    return build()
}