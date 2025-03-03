package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KafkaTopologyConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.StateStoreName
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.namespaceOrDefaultForLocal
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
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import java.util.*

val STATE_STORE_NAME: StateStoreName = StateStoreName("internal_state")
typealias InternalStateStore = KeyValueStore<UUID, InternTilstand>

fun ProcessorContext<*, *>.getStateStore(stateStoreName: StateStoreName): InternalStateStore =
    getStateStore(stateStoreName.value)

private val logger = buildNamedLogger("bekreftelse.varsler.topology")

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
    kafkaTopicsConfig: KafkaTopologyConfig,
    varselService: VarselService,
    varselMeldingBygger: VarselMeldingBygger
): StreamsBuilder {
    with(kafkaTopicsConfig) {
        stream<Long, Periode>(periodeTopic)
            .filter { _, periode -> periode.avsluttet == null }
            .genericProcess<Long, Periode, Long, Unit>("lagre_periode_data", STATE_STORE_NAME.value) { (_, periode) ->
                varselService.mottaPeriode(periode)

                val stateStore = getStateStore(STATE_STORE_NAME)
                val gjeldeneTilstand = stateStore[periode.id]
                val nyTilstand = periode.asInternTilstand(gjeldeneTilstand)
                if (nyTilstand != gjeldeneTilstand) {
                    stateStore.put(periode.id, nyTilstand)
                }
            }

        /*
        stream(bekreftelseHendelseTopic, Consumed.with(Serdes.Long(), BekreftelseHendelseSerde()))
            .mapWithContext("bekreftelse-hendelse-mottatt", STATE_STORE_NAME.value) { hendelse ->
                varselService.mottaBekreftelseHendelse(hendelse)

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
            }
            .foreach { key, meldinger ->
                logger.debug("Sender meldinger: key: {}, value: {}", key, meldinger)
            }*/
        // TODO: Disablet midlertidig .to(tmsVarselTopic, Produced.with(Serdes.String(), Serdes.String()))
    }
    return this
}

fun StreamsBuilder.varselHendelserKafkaTopology(
    runtimeEnvironment: RuntimeEnvironment,
    kafkaTopicsConfig: KafkaTopologyConfig,
    meterRegistry: MeterRegistry,
    varselService: VarselService
): StreamsBuilder {
    with(kafkaTopicsConfig) {
        stream(tmsVarselHendelseTopic, Consumed.with(Serdes.String(), VarselHendelseJsonSerde()))
            .filter { _, hendelse -> hendelse.namespace == runtimeEnvironment.namespaceOrDefaultForLocal() }
            .peek { _, hendelse -> meterRegistry.varselHendelseCounter(hendelse) }
            .filter { _, hendelse -> hendelse.varseltype == VarselType.OPPGAVE }
            .foreach { key, hendelse ->
                logger.debug("Mottok varsel-hendelse: key: {}, value: {}", key, hendelse)
                varselService.mottaVarselHendelse(hendelse)
            }
    }
    return this
}
