package no.nav.paw.kafkakeymaintenance.pdlprocessor

import arrow.core.partially1
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.*
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.paw.kafkakeymaintenance.vo.genererAvviksMelding
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.streams.processor.api.Record
import org.slf4j.LoggerFactory
import java.time.Instant

fun createSpanFromTraceparent(traceparent: String): Context {
    val propagator: TextMapPropagator = GlobalOpenTelemetry.getPropagators().textMapPropagator
    return propagator.extract(Context.current(), traceparent, object : TextMapGetter<String> {
        override fun keys(carrier: String): Iterable<String> = listOf("traceparent")
        override fun get(carrier: String?, key: String): String? = carrier
    })
}

private val spanLinkLogger = LoggerFactory.getLogger("span_link_logger")

@WithSpan(
    value = "process_pdl_aktor_v2_record",
    kind = SpanKind.INTERNAL
)
fun processPdlRecord(
    meterRegistry: PrometheusMeterRegistry,
    aktorTopic: String,
    hentAlias: (List<String>) -> List<LokaleAlias>,
    perioder: Perioder,
    record: Record<String, Aktor>
): List<HendelseRecord<Hendelse>> {
    record.headers().lastHeader("traceparent")
        .also { if (it == null) spanLinkLogger.warn("No traceparent header: headers: {}", record.headers().toList().map {h -> h.key() }) }
        ?.let { traceparent -> createSpanFromTraceparent(String(traceparent.value(), Charsets.UTF_8)) }
        ?.also { context ->
            if (context is SpanContext) {
                spanLinkLogger.info("Linking span with context: {}", context.traceId)
                Span.current().addLink(context)
            } else {
                spanLinkLogger.warn("Not a span context: {}", context::class.java)
            }
        }
    val metadata = metadata(
        kilde = aktorTopic,
        tidspunkt = Instant.now(),
        tidspunktFraKilde = TidspunktFraKilde(
            tidspunkt = Instant.ofEpochMilli(record.timestamp()),
            avviksType = AvviksType.FORSINKELSE
        )
    )
    return prosesser(meterRegistry, hentAlias, record.value(), perioder, metadata)
}

fun prosesser(
    meterRegistry: PrometheusMeterRegistry,
    hentAlias: (List<String>) -> List<LokaleAlias>,
    aktor: Aktor,
    perioder: Perioder,
    metadata: Metadata
) = (hentData(hentAlias, aktor)
    .takeIf(::harAvvik)
    ?.let(::genererAvviksMelding)
    ?.let(perioder::hentPerioder)
    ?.also { (_, perioder) ->
        Span.current()
            .setAttribute("perioder", perioder.size.toLong())
            .setAttribute("aktive_perioder", perioder.filter { p -> p.erAktiv }.size.toLong())
    }
    .also { avvikOgPerioder ->
        meterRegistry
            .counter(
                "paw_kafka_key_maintenance_aktor_consumer_v1",
                listOf(
                    Tag.of("avvik", (avvikOgPerioder != null).toString()),
                    Tag.of("perioder", avvikOgPerioder?.perioder?.size.toBucket()),
                    Tag.of("aktive_perioder", avvikOgPerioder?.perioder?.filter { p -> p.erAktiv }?.size.toBucket()),
                    Tag.of("lokale_alias", avvikOgPerioder?.avviksMelding?.lokaleAlias?.size.toBucket()),
                    Tag.of("pdl_identiteter", avvikOgPerioder?.avviksMelding?.pdlIdentitetsnummer?.size.toBucket()),
                    Tag.of("frie_identer", avvikOgPerioder?.avviksMelding?.lokaleAliasSomIkkeSkalPekePaaPdlPerson()?.size.toBucket())
                )
            ).increment()
    }
    ?.let(::genererIdOppdatering)
    ?.let(::genererHendelser.partially1(metadata))
    ?: emptyList())

fun Int?.toBucket(): String =
    when {
        this == null -> "NAN"
        this < 10 -> toString()
        else -> "10+"
    }