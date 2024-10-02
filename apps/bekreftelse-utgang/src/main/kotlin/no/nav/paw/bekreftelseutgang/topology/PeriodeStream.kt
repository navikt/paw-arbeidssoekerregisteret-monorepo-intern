package no.nav.paw.bekreftelseutgang.topology

import no.nav.paw.bekreftelseutgang.config.ApplicationConfig
import org.apache.kafka.streams.StreamsBuilder
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.streams.mapNonNull
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

fun StreamsBuilder.buildPeriodeStream(applicationConfig: ApplicationConfig){
    with(applicationConfig.kafkaTopology){
        stream<Long, Periode>(periodeTopic)
            .mapNonNull(
                "lagreEllerSlettPeriode",
                internStateStoreName
            ) { periode ->
                val stateStore: KeyValueStore<UUID, String> = getStateStore(internStateStoreName)
                if(periode.avsluttet == null) {
                    stateStore.put(periode.id, periode.identitetsnummer)
                } else {
                    stateStore.delete(periode.id)
                }
            }
    }
}