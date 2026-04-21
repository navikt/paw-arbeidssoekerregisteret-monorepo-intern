package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.exception.PeriodeIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.model.InternalState
import no.nav.paw.arbeidssoekerregisteret.model.VarselMelding
import no.nav.paw.arbeidssoekerregisteret.model.asPeriodeHendelse
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.topology.store.INTERNAL_STATE_STORE
import no.nav.paw.arbeidssoekerregisteret.topology.store.InternalStateStore
import no.nav.paw.arbeidssoekerregisteret.utils.Source
import no.nav.paw.arbeidssoekerregisteret.utils.oppgaveVarselCounter
import no.nav.paw.arbeidssoekerregisteret.utils.readBekreftelseHendelseCounter
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.kafka.processor.genericProcess
import no.nav.paw.kafka.processor.mapKeyAndValue
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import java.util.*

private const val BEKREFTELSE_PERIODE_PROCESSOR = "processPerioderForBekreftelser"
private const val BEKREFTELSE_HENDELSE_PROCESSOR = "processBekreftelseHendelser"

fun StreamsBuilder.addBekreftelseHendelseStream(
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