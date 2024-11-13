package no.nav.paw.kafkakeymaintenance.pdlprocessor

import arrow.core.partially1
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.*
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.paw.kafkakeymaintenance.vo.*
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.streams.processor.api.Record
import java.time.Instant

@WithSpan(
    value = "process_pdl_aktor_v2_record",
    kind = SpanKind.CONSUMER
)
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

fun prosesser(
    hentAlias: (List<String>) -> List<LokaleAlias>,
    aktor: Aktor,
    perioder: Perioder,
    metadata: Metadata
) = (hentData(hentAlias, aktor)
    .takeIf(::harAvvik)
    .also { Span.current().addEvent("Avvik: ${it != null}")}
    ?.let(::genererAvviksMelding)
    ?.let(perioder::hentPerioder)
    ?.also {
        Span.current()
            .addEvent("Perioder/Aktive: ${it.perioder.size}/${it.perioder.filter { p -> p.erAktiv }.size}")
    }
    ?.let(::genererIdOppdatering)
    ?.let(::genererHendelser.partially1(metadata))
    ?: emptyList())
