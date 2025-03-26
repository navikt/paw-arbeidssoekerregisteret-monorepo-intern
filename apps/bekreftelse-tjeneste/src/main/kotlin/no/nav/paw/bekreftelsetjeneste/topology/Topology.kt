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
    buildPeriodeStream(
        prometheusMeterRegistry = applicationContext.prometheusMeterRegistry,
        applicationConfig = applicationContext.applicationConfig,
        kafaKeysClient = applicationContext.kafkaKeysClient
    )
    buildBekreftelseStream(
        prometheusMeterRegistry = applicationContext.prometheusMeterRegistry,
        applicationConfig = applicationContext.applicationConfig,
        bekreftelseKonfigurasjon = applicationContext.bekreftelseKonfigurasjon,
        oddetallPartallMap = applicationContext.oddetallPartallMap
    )
    byggBekreftelsePaaVegneAvStroem(
        deaktiverUtmeldingVedStopp = applicationContext.applicationConfig.deaktiverUtmeldingVedStopp,
        registry = applicationContext.prometheusMeterRegistry,
        kafkaTopologyConfig = applicationContext.applicationConfig.kafkaTopology,
        bekreftelseHendelseSerde = applicationContext.bekreftelseHendelseSerde,
        bekreftelseKonfigurasjon = applicationContext.bekreftelseKonfigurasjon
    )
    return build()
}

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse = runBlocking {
    getIdAndKey(identitetsnummer)
}
