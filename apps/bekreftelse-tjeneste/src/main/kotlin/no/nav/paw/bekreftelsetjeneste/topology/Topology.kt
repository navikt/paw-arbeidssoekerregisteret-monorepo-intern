package no.nav.paw.bekreftelsetjeneste.topology

import kotlinx.coroutines.runBlocking
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.bekreftelsetjeneste.context.ApplicationContext
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstandSerde
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import java.util.*

typealias StateStore = KeyValueStore<UUID, InternTilstand>

fun buildTopology(
    applicationContext: ApplicationContext
): Topology = StreamsBuilder().apply {
    buildInternStateStore(applicationContext.applicationConfig)
    buildPeriodeStream(applicationContext.applicationConfig, applicationContext.kafkaKeysClient)
    buildBekreftelseStream(applicationContext.applicationConfig)
}.build()

fun StreamsBuilder.buildInternStateStore(applicationConfig: ApplicationConfig) {
    with(applicationConfig.kafkaTopology) {
        addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(internStateStoreName),
                Serdes.UUID(),
                InternTilstandSerde()
            )
        )
    }
}

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse = runBlocking {
    getIdAndKey(identitetsnummer)
}
