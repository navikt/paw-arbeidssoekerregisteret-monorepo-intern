package no.nav.paw.bekreftelsetjeneste.topology

import kotlinx.coroutines.runBlocking
import no.nav.paw.bekreftelsetjeneste.paavegneav.PaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.context.ApplicationContext
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

typealias BekreftelseTilstandStateStore = KeyValueStore<UUID, BekreftelseTilstand>
typealias PaaVegneAvTilstandStateStore = KeyValueStore<UUID, PaaVegneAvTilstand>

fun StreamsBuilder.buildTopology(
    applicationContext: ApplicationContext
): Topology {
    buildPeriodeStream(applicationContext.applicationConfig, applicationContext.kafkaKeysClient)
    buildBekreftelseStream(applicationContext.prometheusMeterRegistry, applicationContext.applicationConfig)
    byggBekreftelsePaaVegneAvStroem(
        registry = applicationContext.prometheusMeterRegistry,
        kafkaTopologyConfig = applicationContext.applicationConfig.kafkaTopology,
        bekreftelseHendelseSerde = applicationContext.bekreftelseHendelseSerde
    )
    return build()
}

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse = runBlocking {
    getIdAndKey(identitetsnummer)
}
