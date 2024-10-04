package no.nav.paw.bekreftelseutgang.topology

import no.nav.paw.bekreftelseutgang.config.ApplicationConfig
import org.apache.kafka.streams.StreamsBuilder
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.bekreftelseutgang.tilstand.InternTilstand
import no.nav.paw.bekreftelseutgang.tilstand.StateStore
import no.nav.paw.bekreftelseutgang.tilstand.generateAvsluttetEventIfStateIsComplete
import no.nav.paw.config.kafka.streams.mapNonNull
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.Produced

fun StreamsBuilder.buildPeriodeStream(applicationConfig: ApplicationConfig){
    with(applicationConfig.kafkaTopology){
        stream<Long, Periode>(periodeTopic)
            .mapNonNull<Long, Periode, Hendelse>(
                "periodeStream",
                stateStoreName
            ) { periode ->
                val stateStore: StateStore = getStateStore(stateStoreName)
                val currentState = stateStore[periode.id] ?: InternTilstand(null, null)

                when {
                    currentState.identitetsnummer != null && periode.avsluttet != null -> {
                        stateStore.delete(periode.id)
                        null
                    }
                    else -> {
                        val newState = currentState.copy(identitetsnummer = periode.identitetsnummer)
                        stateStore.put(periode.id, newState)

                        newState.generateAvsluttetEventIfStateIsComplete(applicationConfig)
                    }
                }
            }.to(hendelseloggTopic, Produced.with(Serdes.Long(), HendelseSerde()))
    }
}
