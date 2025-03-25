package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.exception.PeriodeIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.model.InternalState
import no.nav.paw.arbeidssoekerregisteret.model.VarselMelding
import no.nav.paw.arbeidssoekerregisteret.model.asPeriodeHendelse
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.utils.InternalStateSerde
import no.nav.paw.arbeidssoekerregisteret.utils.Source
import no.nav.paw.arbeidssoekerregisteret.utils.VarselHendelseSerde
import no.nav.paw.arbeidssoekerregisteret.utils.beskjedVarselCounter
import no.nav.paw.arbeidssoekerregisteret.utils.oppgaveVarselCounter
import no.nav.paw.arbeidssoekerregisteret.utils.readBekreftelseHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.readPeriodeCounter
import no.nav.paw.arbeidssoekerregisteret.utils.readVarselHendelseCounter
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.namespaceOrDefaultForLocal
import no.nav.paw.kafka.processor.genericProcess
import no.nav.paw.kafka.processor.mapKeyAndValue
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import java.util.*

private const val PERIODE_PROCESSOR = "processPerioder"
private const val BEKREFTELSE_PERIODE_PROCESSOR = "processPerioderForBekreftelser"
private const val BEKREFTELSE_HENDELSE_PROCESSOR = "processBekreftelseHendelser"

const val INTERNAL_STATE_STORE = "internalStateStore"
typealias InternalStateStore = KeyValueStore<UUID, InternalState>

fun StreamsBuilder.internalStateStore(): StreamsBuilder {
    addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(INTERNAL_STATE_STORE),
            Serdes.UUID(),
            InternalStateSerde()
        )
    )
    return this
}

fun StreamsBuilder.periodeKafkaTopology(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    varselService: VarselService
): StreamsBuilder {
    with(applicationConfig) {
        stream<Long, Periode>(periodeTopic)
            .peek { _, periode -> meterRegistry.readPeriodeCounter(periode) }
            .genericProcess<Long, Periode, UUID, VarselMelding>(PERIODE_PROCESSOR) { record ->
                val periode = record.value()
                val meldinger = varselService.mottaPeriode(periode)
                meldinger.forEach { melding -> forward(record.withKey(melding.varselId).withValue(melding)) }
            }
            .peek { _, melding -> meterRegistry.beskjedVarselCounter(Source.KAFKA, melding) }
            .mapKeyAndValue("mapVarselMelding") { key, melding -> key.toString() to melding.value }
            .to(tmsVarselTopic, Produced.with(Serdes.String(), Serdes.String()))
    }
    return this
}

fun StreamsBuilder.bekreftelseKafkaTopology(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    varselService: VarselService
): StreamsBuilder {
    with(applicationConfig) {
        stream<Long, Periode>(periodeTopic)
            .genericProcess<Long, Periode, Long, Periode>(
                BEKREFTELSE_PERIODE_PROCESSOR,
                INTERNAL_STATE_STORE
            ) { record ->
                val periode = record.value().asPeriodeHendelse()
                val internalStateStore = getStateStore<InternalStateStore>(INTERNAL_STATE_STORE)
                internalStateStore.put(periode.periodeId, InternalState(periode))
            }

        stream(bekreftelseHendelseTopic, Consumed.with(Serdes.Long(), BekreftelseHendelseSerde()))
            .peek { _, hendelse -> meterRegistry.readBekreftelseHendelseCounter(hendelse) }
            .genericProcess<Long, BekreftelseHendelse, UUID, VarselMelding>(
                BEKREFTELSE_HENDELSE_PROCESSOR,
                INTERNAL_STATE_STORE
            ) { record ->
                val hendelse = record.value()
                val internalStateStore = getStateStore<InternalStateStore>(INTERNAL_STATE_STORE)
                val internalState: InternalState? = internalStateStore.get(hendelse.periodeId)
                if (internalState == null) {
                    throw PeriodeIkkeFunnetException("Ingen periode mottatt for hendelse ${hendelse.hendelseType}")
                } else {
                    val periode = internalState.periode
                    val meldinger = varselService.mottaBekreftelseHendelse(periode, hendelse)
                    meldinger.forEach { melding -> forward(record.withKey(melding.varselId).withValue(melding)) }
                }
            }
            .peek { _, melding -> meterRegistry.oppgaveVarselCounter(Source.KAFKA, melding) }
            .mapKeyAndValue("mapVarselMelding") { key, melding -> key.toString() to melding.value }
            .to(tmsVarselTopic, Produced.with(Serdes.String(), Serdes.String()))
    }
    return this
}

fun StreamsBuilder.varselHendelserKafkaTopology(
    runtimeEnvironment: RuntimeEnvironment,
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    varselService: VarselService
): StreamsBuilder {
    with(applicationConfig) {
        stream(tmsVarselHendelseTopic, Consumed.with(Serdes.String(), VarselHendelseSerde()))
            .filter { _, hendelse -> hendelse.namespace == runtimeEnvironment.namespaceOrDefaultForLocal() }
            .peek { _, hendelse -> meterRegistry.readVarselHendelseCounter(hendelse) }
            .foreach { _, hendelse -> varselService.mottaVarselHendelse(hendelse) }
    }
    return this
}
