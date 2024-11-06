package no.nav.paw.kafkakeymaintenance.functions

import arrow.core.partially1
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.ANTALL_PARTISJONER
import no.nav.paw.kafkakeymaintenance.kafka.Topic
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import no.nav.paw.kafkakeymaintenance.kafka.updateHwm
import no.nav.paw.kafkakeymaintenance.metadata
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.paw.kafkakeymaintenance.vo.*
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Suppressed
import org.apache.kafka.streams.processor.api.Record
import java.time.Duration
import java.time.Instant


fun processPdlRecord(
    aktorTopic: String,
    hentAlias: (List<String>) -> List<LokaleAlias>,
    perioder: Perioder,
    record: Record<String, Aktor>
): List<HendelseRecord<Hendelse>> {
    val metadata = metadata(
        kilde = aktorTopic,
        tidspunkt = Instant.now(),
        tidspunktFraKilde = TidspunktFraKilde(
            tidspunkt = Instant.ofEpochMilli(record.timestamp()),
            avviksType = AvviksType.FORSINKELSE
        )
    )
    return prosesser(hentAlias, record.value(), perioder, metadata)
}

fun Perioder.hentPerioder(avviksMelding: AvviksMelding): AvvvikOgPerioder {
    val identiteter = avviksMelding.lokaleAlias
        .map { it.identitetsnummer } +
            avviksMelding.pdlIdentitetsnummer
    return AvvvikOgPerioder(
        avviksMelding = avviksMelding,
        perioder = get(identiteter)
    )
}

fun prosesser(
    hentAlias: (List<String>) -> List<LokaleAlias>,
    aktor: Aktor,
    perioder: Perioder,
    metadata: Metadata
) = (hentData(hentAlias, aktor)
    .takeIf(::harAvvik)
    ?.let(::genererAvviksMelding)
    ?.let(perioder::hentPerioder)
    ?.let(::genererIdOppdatering)
    ?.let(::genererHendelser.partially1(metadata))
    ?: emptyList())
