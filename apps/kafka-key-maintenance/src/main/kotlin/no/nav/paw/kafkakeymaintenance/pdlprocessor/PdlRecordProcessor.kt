package no.nav.paw.kafkakeymaintenance.pdlprocessor

import arrow.core.partially1
import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
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
import no.nav.paw.kafkakeymaintenance.kafka.Topic
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.*
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.Data
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.paw.kafkakeymaintenance.vo.genererAvviksMelding
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.common.serialization.Deserializer
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

fun procesAktorMelding(
    meterRegistry: PrometheusMeterRegistry,
    hentAlias: (List<String>) -> List<LokaleAlias>,
    aktorTopic: Topic,
    perioder: Perioder,
    aktorDeserializer: Deserializer<Aktor>,
    data: Data
): List<HendelseRecord<Hendelse>> {
    val metadata = metadata(
        kilde = aktorTopic.value,
        tidspunkt = Instant.now(),
        tidspunktFraKilde = TidspunktFraKilde(
            tidspunkt = data.time,
            avviksType = AvviksType.FORSINKELSE
        )
    )
    val aktor = aktorDeserializer.deserialize(aktorTopic.value, data.data)
    return procesAktorMelding(
        meterRegistry = meterRegistry,
        hentAlias = hentAlias,
        aktor = aktor,
        perioder = perioder,
        metadata = metadata
    )
}

fun procesAktorMelding(
    meterRegistry: PrometheusMeterRegistry,
    hentAlias: (List<String>) -> List<LokaleAlias>,
    perioder: Perioder,
    aktor: Aktor,
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