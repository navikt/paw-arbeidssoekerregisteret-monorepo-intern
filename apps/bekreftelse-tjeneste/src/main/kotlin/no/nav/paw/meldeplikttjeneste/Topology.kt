package no.nav.paw.meldeplikttjeneste

import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.meldeplikttjeneste.tilstand.InternTilstand
import no.nav.paw.rapportering.ansvar.v1.AnsvarEndret
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

typealias StateStore = KeyValueStore<UUID, InternTilstand>

context(ApplicationConfiguration, ApplicationContext)
fun StreamsBuilder.appTopology(): Topology {
    processPeriodeTopic()
    processAnsvarTopic()
    processRapporteringsMeldingTopic()

    return build()
}
