package no.nav.paw.kafkakeymaintenance.pdlprocessor

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.config.kafka.streams.mapRecord
import no.nav.paw.config.kafka.streams.supressByWallClock
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced

fun StreamsBuilder.buildAktorTopology(
    meterRegistry: PrometheusMeterRegistry,
    aktorTopologyConfig: AktorTopologyConfig,
    perioder: Perioder,
    hentAlias: (List<String>) -> List<LokaleAlias>,
    aktorSerde: Serde<Aktor>
) {
    stream(aktorTopologyConfig.aktorTopic, Consumed.with(Serdes.String(), aktorSerde))
        .supressByWallClock(
            name = aktorTopologyConfig.stateStoreName,
            duration = aktorTopologyConfig.supressionDelay,
            checkInterval = aktorTopologyConfig.interval
        ).mapRecord("aktor_til_hendelse") { record ->
            record.withValue(
                processPdlRecord(
                    meterRegistry = meterRegistry,
                    aktorTopic = aktorTopologyConfig.aktorTopic,
                    hentAlias = hentAlias,
                    perioder = perioder,
                    record = record
                )
            )
        }.flatMap { _, hendelser ->
            hendelser.map { hendelseRecord ->
                KeyValue.pair(
                    hendelseRecord.key,
                    hendelseRecord.hendelse
                )
            }
        }
        .mapRecord("set_timestamp") { it.withTimestamp(System.currentTimeMillis()) }
        .to(aktorTopologyConfig.hendelseloggTopic, Produced.with(Serdes.Long(), HendelseSerde()))
}

