package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KafkaTopicsConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.StateStoreName
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.VarselHendelse
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.kafka.processor.genericProcess
import no.nav.paw.kafka.processor.mapKeyAndValue
import no.nav.paw.kafka.processor.mapWithContext
import no.nav.paw.kafka.streams.record.component1
import no.nav.paw.kafka.streams.record.component2
import no.nav.paw.logging.logger.buildNamedLogger
import no.nav.paw.serialization.kafka.buildJacksonSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import java.util.*

const val BEKREFTELSE_STREAM_SUFFIX = "beta-v2"
const val VARSEL_HENDELSE_STREAM_SUFFIX = "varsel-hendelser-beta-v2"
val STATE_STORE_NAME: StateStoreName = StateStoreName("internal_state")
typealias InternalStateStore = KeyValueStore<UUID, InternTilstand>

fun ProcessorContext<*, *>.getStateStore(stateStoreName: StateStoreName): InternalStateStore =
    getStateStore(stateStoreName.value)

private val logger = LoggerFactory.getLogger("bekreftelse.varsler.topology")

fun StreamsBuilder.internStateStore(): StreamsBuilder {
    addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(STATE_STORE_NAME.value),
            Serdes.UUID(),
            buildJacksonSerde<InternTilstand>()
        )
    )
    return this
}

fun StreamsBuilder.bekreftelseKafkaTopology(
    varselMeldingBygger: VarselMeldingBygger,
    kafkaTopicsConfig: KafkaTopicsConfig
): StreamsBuilder {
    with(kafkaTopicsConfig) {
        stream<Long, Periode>(periodeTopic)
            .filter { _, periode -> periode.avsluttet == null } // Filtrer vekk avsluttede perioder
            .genericProcess<Long, Periode, Long, Unit>("lagre_periode_data", STATE_STORE_NAME.value) { (_, periode) ->
                val stateStore = getStateStore(STATE_STORE_NAME)
                val gjeldeneTilstand = stateStore[periode.id]
                val nyTilstand = periode.asInternTilstand(gjeldeneTilstand)
                if (nyTilstand != gjeldeneTilstand) {
                    stateStore.put(periode.id, nyTilstand)
                }
            }

        stream(bekreftelseHendelseTopic, Consumed.with(Serdes.Long(), BekreftelseHendelseSerde()))
            .mapWithContext("bekreftelse-hendelse-mottatt", STATE_STORE_NAME.value) { hendelse ->
                val stateStore = getStateStore(STATE_STORE_NAME)
                val gjeldeneTilstand = stateStore[hendelse.periodeId]
                if (gjeldeneTilstand == null) {
                    logger.warn(
                        "Ignorer {}::{} for periode som ikke er aktiv: {}",
                        hendelse.hendelseType,
                        hendelse.hendelseId,
                        hendelse.periodeId
                    )
                    emptyList()
                } else {
                    val (nyTilstand, meldinger) = gjeldeneTilstand.asOppgaveMeldinger(hendelse, varselMeldingBygger)
                    when {
                        nyTilstand == null -> stateStore.delete(hendelse.periodeId)
                        nyTilstand != gjeldeneTilstand -> stateStore.put(hendelse.periodeId, nyTilstand)
                    }
                    meldinger
                }
            }
            .flatMapValues { _, meldinger -> meldinger }
            .mapKeyAndValue("map_til_utgaaende") { _, melding ->
                melding.varselId.toString() to melding.value
            }.to(tmsOppgaveTopic, Produced.with(Serdes.String(), Serdes.String()))
    }
    return this
}

private val tempLogger = buildNamedLogger("kafka.tms.varsel.hendelse")

fun StreamsBuilder.varselHendelserKafkaTopology(
    kafkaTopicsConfig: KafkaTopicsConfig,
    meterRegistry: MeterRegistry
): StreamsBuilder {
    with(kafkaTopicsConfig) {
        stream<String, VarselHendelse>(tmsVarselHendelseTopic)
            .filter { _, value -> value.namespace == "paw" }
            .foreach { key, value ->
                meterRegistry.varselHendelseCounter(value)
                tempLogger.info("TMS Varselhendelse: $key -> $value")
            }
    }
    return this
}
