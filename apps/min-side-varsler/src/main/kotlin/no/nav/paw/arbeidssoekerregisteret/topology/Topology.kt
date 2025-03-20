package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.utils.Source
import no.nav.paw.arbeidssoekerregisteret.utils.VarselHendelseJsonSerde
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
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.ValueJoiner

fun StreamsBuilder.periodeKafkaTopology(
    runtimeEnvironment: RuntimeEnvironment,
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    varselService: VarselService
): StreamsBuilder {
    with(applicationConfig) {
        stream<Long, Periode>(periodeTopic)
            .peek { _, periode -> meterRegistry.readPeriodeCounter(periode) }
            .flatMapValues { _, periode -> varselService.mottaPeriode(periode) }
            .peek { _, melding -> meterRegistry.beskjedVarselCounter(runtimeEnvironment, Source.KAFKA, melding) }
            .map { _, melding -> KeyValue.pair(melding.varselId.toString(), melding.value) }
            .to(tmsVarselTopic, Produced.with(Serdes.String(), Serdes.String()))
    }
    return this
}

class BekreftelseValueJoiner : ValueJoiner<BekreftelseHendelse, Periode, Pair<Periode?, BekreftelseHendelse?>> {
    override fun apply(bekreftelse: BekreftelseHendelse?, periode: Periode?): Pair<Periode?, BekreftelseHendelse?> {
        return periode to bekreftelse
    }
}

fun StreamsBuilder.bekreftelseKafkaTopology(
    runtimeEnvironment: RuntimeEnvironment,
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    varselService: VarselService
): StreamsBuilder {
    with(applicationConfig) {
        val periodeTable = table<Long, Periode>(periodeTopic)
        val bekreftelseStream = stream(
            bekreftelseHendelseTopic,
            Consumed.with(Serdes.Long(), BekreftelseHendelseSerde())
        )

        bekreftelseStream.leftJoin(periodeTable, BekreftelseValueJoiner())
            .peek { _, (_, hendelse) -> meterRegistry.readBekreftelseHendelseCounter(hendelse) }
            .flatMapValues { _, value -> varselService.mottaBekreftelseHendelse(value) }
            .peek { _, melding -> meterRegistry.oppgaveVarselCounter(runtimeEnvironment, Source.KAFKA, melding) }
            .map { _, melding -> KeyValue.pair(melding.varselId.toString(), melding.value) }
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
        stream(tmsVarselHendelseTopic, Consumed.with(Serdes.String(), VarselHendelseJsonSerde()))
            .filter { _, hendelse -> hendelse.namespace == runtimeEnvironment.namespaceOrDefaultForLocal() }
            .peek { _, hendelse -> meterRegistry.readVarselHendelseCounter(hendelse) }
            .foreach { _, hendelse -> varselService.mottaVarselHendelse(hendelse) }
    }
    return this
}
