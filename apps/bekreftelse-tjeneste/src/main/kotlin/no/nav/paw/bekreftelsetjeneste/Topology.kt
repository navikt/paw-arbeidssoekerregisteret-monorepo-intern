package no.nav.paw.bekreftelsetjeneste

import no.nav.paw.bekreftelsetjeneste.context.ApplicationContext
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

typealias StateStore = KeyValueStore<UUID, InternTilstand>

fun StreamsBuilder.appTopology(applicationContext: ApplicationContext): Topology {
    processPeriodeTopic(applicationContext)
    processBekreftelseMeldingTopic(applicationContext)
    return build()
}
