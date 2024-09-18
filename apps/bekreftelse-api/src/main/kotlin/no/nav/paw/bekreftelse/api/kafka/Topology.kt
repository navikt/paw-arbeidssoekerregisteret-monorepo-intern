package no.nav.paw.bekreftelse.api.kafka

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.utils.buildInternStateSerde
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.state.Stores

fun buildBekreftelseTopology(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry
): Topology = StreamsBuilder().apply {
    addInternStateStore(applicationConfig)
    addBekreftelseKStream(applicationConfig, meterRegistry)
}.build()

private fun StreamsBuilder.addInternStateStore(applicationConfig: ApplicationConfig) {
    addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName),
            Serdes.Long(),
            buildInternStateSerde(),
        )
    )
}

private fun StreamsBuilder.addBekreftelseKStream(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry
) {
    stream(
        applicationConfig.kafkaTopology.bekreftelseHendelsesloggTopic,
        Consumed.with(Serdes.Long(), BekreftelseHendelseSerde())
    )
        .oppdaterBekreftelseHendelseState(applicationConfig.kafkaTopology.internStateStoreName, meterRegistry)
}