package no.nav.paw.arbeidssoekerregisteret.topology.store

import no.nav.paw.arbeidssoekerregisteret.model.InternalState
import no.nav.paw.arbeidssoekerregisteret.utils.InternalStateSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import java.util.UUID

const val INTERNAL_STATE_STORE = "internalStateStore"
typealias InternalStateStore = KeyValueStore<UUID, InternalState>

fun StreamsBuilder.addInternalStateStore(): StreamsBuilder {
    addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(INTERNAL_STATE_STORE),
            Serdes.UUID(),
            InternalStateSerde()
        )
    )
    return this
}