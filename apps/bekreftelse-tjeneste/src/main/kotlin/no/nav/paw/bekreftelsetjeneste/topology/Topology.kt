package no.nav.paw.bekreftelsetjeneste.topology

import kotlinx.coroutines.runBlocking
import no.nav.paw.bekreftelsetjeneste.context.ApplicationContext
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstandSerde
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import java.util.*

typealias StateStore = KeyValueStore<UUID, InternTilstand>

fun buildTopology(
    applicationContext: ApplicationContext,
    keyValueStateStoreSupplier: (String) -> KeyValueBytesStoreSupplier
): Topology = StreamsBuilder().apply {
    addStateStore(
        Stores.keyValueStoreBuilder(
            keyValueStateStoreSupplier(applicationContext.applicationConfig.kafkaTopology.internStateStoreName),
            Serdes.UUID(),
            InternTilstandSerde()
        )
    )
    buildPeriodeStream(applicationContext.applicationConfig, applicationContext.kafkaKeysClient)
    buildBekreftelseStream(applicationContext.applicationConfig)
}.build()

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse = runBlocking {
    getIdAndKey(identitetsnummer)
}
