package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.KafkaTopologyConfig
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.utils.VarselHendelseJsonSerde
import no.nav.paw.arbeidssoekerregisteret.utils.bekreftelseHendelseCounter
import no.nav.paw.arbeidssoekerregisteret.utils.periodeCounter
import no.nav.paw.arbeidssoekerregisteret.utils.varselCounter
import no.nav.paw.arbeidssoekerregisteret.utils.varselHendelseCounter
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.namespaceOrDefaultForLocal
import no.nav.paw.logging.logger.buildNamedLogger
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced

private val logger = buildNamedLogger("bekreftelse.varsler.topology")

fun StreamsBuilder.bekreftelseKafkaTopology(
    runtimeEnvironment: RuntimeEnvironment,
    kafkaTopicsConfig: KafkaTopologyConfig,
    meterRegistry: MeterRegistry,
    varselService: VarselService
): StreamsBuilder {
    with(kafkaTopicsConfig) {
        stream<Long, Periode>(periodeTopic)
            .peek { _, periode -> meterRegistry.periodeCounter(periode) }
            .foreach { _, periode ->
                varselService.mottaPeriode(periode)
            }

        stream(bekreftelseHendelseTopic, Consumed.with(Serdes.Long(), BekreftelseHendelseSerde()))
            .peek { _, hendelse -> meterRegistry.bekreftelseHendelseCounter(hendelse) }
            .flatMapValues { _, hendelse ->
                varselService.mottaBekreftelseHendelse(hendelse)
            }
            .peek { _, melding -> meterRegistry.varselCounter(runtimeEnvironment, melding) }
            .filter { _, _ -> false } // TODO: Disable utsending av varsler
            .map { _, melding -> KeyValue.pair(melding.varselId.toString(), melding.value) }
            .to(tmsVarselTopic, Produced.with(Serdes.String(), Serdes.String()))
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
            .foreach { _, hendelse ->
                varselService.mottaVarselHendelse(hendelse)
            }
    }
    return this
}
