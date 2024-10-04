package no.nav.paw.bekreftelseutgang.topology

import no.nav.paw.bekreftelseutgang.config.ApplicationConfig
import org.apache.kafka.streams.StreamsBuilder
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.bekreftelseutgang.tilstand.InternTilstand
import no.nav.paw.bekreftelseutgang.tilstand.StateStore
import no.nav.paw.config.kafka.streams.genericProcess
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.Produced

fun StreamsBuilder.buildPeriodeStream(applicationConfig: ApplicationConfig){
    with(applicationConfig.kafkaTopology){
        stream<Long, Periode>(periodeTopic)
            .genericProcess<Long, Periode, Long, Hendelse>(
                "periodeStream",
                stateStoreName
            ) { record ->
                val stateStore: StateStore = getStateStore(stateStoreName)
                val currentState = stateStore[record.value().id]
                if(currentState == null && record.value().avsluttet == null) {
                    stateStore.put(record.value().id, InternTilstand(
                        identitetsnummer = record.value().identitetsnummer,
                        bekreftelseHendelse = null
                    ))

                    return@genericProcess
                }

                if(currentState.bekreftelseHendelse != null) {
                    processBekreftelseHendelse(
                        bekreftelseHendelse = currentState.bekreftelseHendelse,
                        identitetsnummer = record.value().identitetsnummer,
                        applicationConfig = applicationConfig
                    ) ?: return@genericProcess

                    stateStore.delete(record.value().id)
                }

                logger.info("Sender AvsluttetHendelse for periodeId ${record.value().id}")

            }.to(hendelseloggTopic, Produced.with(Serdes.Long(), HendelseSerde()))
    }
}
