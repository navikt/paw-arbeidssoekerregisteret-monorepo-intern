package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KafkaTopics
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.StateStoreName
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.kafka.processor.genericProcess
import no.nav.paw.kafka.processor.mapKeyAndValue
import no.nav.paw.kafka.processor.mapWithContext
import no.nav.paw.kafka.streams.record.component1
import no.nav.paw.kafka.streams.record.component2
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.util.*

typealias InternalStateStore = KeyValueStore<UUID, InternTilstand>

fun ProcessorContext<*, *>.getStateStore(stateStoreName: StateStoreName): InternalStateStore =
    getStateStore(stateStoreName.value)

private val logger = LoggerFactory.getLogger("bekreftelse.varsler.topology")

fun StreamsBuilder.applicationTopology(
    varselMeldingBygger: VarselMeldingBygger,
    kafkaTopics: KafkaTopics,
    stateStoreName: StateStoreName
): Topology {
    stream<Long, Periode>(kafkaTopics.periodeTopic)
        .filter { _, periode -> periode.avsluttet == null }
        .genericProcess<Long, Periode, Long, Unit>("lagre_periode_data", stateStoreName.value) { (_, periode) ->
            val stateStore = getStateStore(stateStoreName)
            val gjeldeneTilstand = stateStore[periode.id]
            val nyTilstand = genererTilstand(gjeldeneTilstand, periode)
            if (nyTilstand != gjeldeneTilstand) {
                stateStore.put(periode.id, nyTilstand)
            }
        }

    stream(
        kafkaTopics.bekreftelseHendelseTopic, Consumed.with(Serdes.Long(), BekreftelseHendelseSerde())
    ).mapWithContext("bekreftelse-hendelse-mottatt", stateStoreName.value) { hendelse ->
        val store = getStateStore(stateStoreName)
        val tilstand = store[hendelse.periodeId]
        if (tilstand == null) {
            logger.warn(
                "Ignorer {}::{} for periode som ikke er aktiv: {}",
                hendelse.hendelseType,
                hendelse.hendelseId,
                hendelse.periodeId
            )
            emptyList()
        } else {
            val (nyTilstand, meldinger) = genererOppgaveMeldinger(tilstand, hendelse, varselMeldingBygger)
            when {
                nyTilstand == null -> store.delete(hendelse.periodeId)
                nyTilstand != tilstand -> store.put(hendelse.periodeId, nyTilstand)
            }
            meldinger
        }
    }
        .flatMapValues { _, meldinger -> meldinger }
        .mapKeyAndValue("map_til_utgaaende") { _, melding ->
            melding.varselId.toString() to melding.value
        }.to(kafkaTopics.tmsOppgaveTopic, Produced.with(Serdes.String(), Serdes.String()))

    return build()
}