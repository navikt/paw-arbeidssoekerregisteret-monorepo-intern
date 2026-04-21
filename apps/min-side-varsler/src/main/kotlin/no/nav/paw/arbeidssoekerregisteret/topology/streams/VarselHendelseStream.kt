package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.utils.VarselHendelseSerde
import no.nav.paw.arbeidssoekerregisteret.utils.readVarselHendelseCounter
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.namespaceOrDefaultForLocal
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed

fun StreamsBuilder.addVarselHendelseStream(
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