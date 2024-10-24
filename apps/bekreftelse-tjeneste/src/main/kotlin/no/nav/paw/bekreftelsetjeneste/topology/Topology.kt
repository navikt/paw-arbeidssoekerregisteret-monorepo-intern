package no.nav.paw.bekreftelsetjeneste.topology

import kotlinx.coroutines.runBlocking
import no.nav.paw.bekreftelsetjeneste.ansvar.Ansvar
import no.nav.paw.bekreftelsetjeneste.context.ApplicationContext
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

typealias InternTilstandStateStore = KeyValueStore<UUID, InternTilstand>
typealias AnsvarStateStore = KeyValueStore<UUID, Ansvar>

fun StreamsBuilder.buildTopology(
    applicationContext: ApplicationContext
): Topology {
    buildPeriodeStream(applicationContext.applicationConfig, applicationContext.kafkaKeysClient)
    buildBekreftelseStream(applicationContext.applicationConfig)
    byggAnsvarsStroem(
        registry = applicationContext.prometheusMeterRegistry,
        kafkaTopologyConfig = applicationContext.applicationConfig.kafkaTopology,
        bekreftelseHendelseSerde = applicationContext.bekreftelseHendelseSerde
    )
    return build()
}

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse = runBlocking {
    getIdAndKey(identitetsnummer)
}
