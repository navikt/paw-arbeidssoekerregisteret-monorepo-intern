package no.nav.paw.bekreftelseutgang.topology

import no.nav.paw.bekreftelseutgang.config.ApplicationConfig
import no.nav.paw.bekreftelseutgang.context.ApplicationContext
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.Stores

fun buildTopology(
    applicationContext: ApplicationContext
): Topology = StreamsBuilder().apply {
    buildInternStateStore(applicationContext.applicationConfig)
    buildPeriodeStream(applicationContext.applicationConfig)
    buildBekreftelseUtgangStream(applicationContext.applicationConfig)
}.build()

fun StreamsBuilder.buildInternStateStore(applicationConfig: ApplicationConfig) {
    with(applicationConfig.kafkaTopology) {
        addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(internStateStoreName),
                Serdes.UUID(),
                Serdes.String()
            )
        )
    }
}
