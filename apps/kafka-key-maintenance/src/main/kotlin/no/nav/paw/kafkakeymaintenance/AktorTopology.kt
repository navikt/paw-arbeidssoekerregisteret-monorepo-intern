package no.nav.paw.kafkakeymaintenance

import kotlinx.coroutines.runBlocking
import no.nav.paw.config.kafka.streams.mapRecord
import no.nav.paw.config.kafka.streams.supressByWallClock
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.functions.processPdlRecord
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import java.time.Duration
import java.time.Instant

fun KafkaKeysClient.hentAlias(identiteter: List<String>): List<LokaleAlias> = runBlocking {
    getAlias(ANTALL_PARTISJONER, identiteter).alias
}

fun StreamsBuilder.buildAktorTopology(
    topic: String,
    stateStoreName: String,
    supressionDelay: Duration,
    interval: Duration = supressionDelay.dividedBy(10),
    perioder: Perioder,
    hentAlias: (List<String>) -> List<LokaleAlias>
) {
    stream<String, Aktor>(topic)
        .supressByWallClock(
            name = stateStoreName,
            duration = supressionDelay,
            checkInterval = interval
        ).mapRecord("aktor_til_hendelse") { record ->
            record.withValue(
                processPdlRecord(
                    aktorTopic = topic,
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
        }.mapRecord("set_timestamp") { it.withTimestamp(Instant.now().epochSecond) }
}