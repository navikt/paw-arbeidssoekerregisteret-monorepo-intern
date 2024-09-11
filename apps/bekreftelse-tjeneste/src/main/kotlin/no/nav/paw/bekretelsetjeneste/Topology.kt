package no.nav.paw.bekretelsetjeneste

import no.nav.paw.bekretelsetjeneste.tilstand.InternTilstand
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

typealias StateStore = KeyValueStore<UUID, InternTilstand>

context(ApplicationConfiguration, ApplicationContext)
fun StreamsBuilder.appTopology(): Topology {
    processPeriodeTopic()
    processAnsvarTopic()
    processBekreftelseMeldingTopic()

    return build()
}
