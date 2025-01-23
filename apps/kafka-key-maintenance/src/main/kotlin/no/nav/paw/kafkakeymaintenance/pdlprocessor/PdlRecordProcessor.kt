package no.nav.paw.kafkakeymaintenance.pdlprocessor

import arrow.core.partially1
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.kafka.Topic
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.*
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.Ident
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.Person
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.paw.kafkakeymaintenance.vo.genererAvviksMelding
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
    person: Person,
    identiter: List<Ident>
): List<HendelseRecord<Hendelse>> {
    val metadata = metadata(
        kilde = aktorTopic.value,
        tidspunkt = Instant.now(),
        tidspunktFraKilde = TidspunktFraKilde(
            tidspunkt = person.tidspunktFraKilde,
            avviksType = AvviksType.FORSINKELSE
        )
    )
    return procesAktorMelding(
        meterRegistry = meterRegistry,
        hentAlias = hentAlias,
        identiter = identiter,
        perioder = perioder,
        metadata = metadata
    )
}

fun procesAktorMelding(
    meterRegistry: PrometheusMeterRegistry,
    hentAlias: (List<String>) -> List<LokaleAlias>,
    perioder: Perioder,
    identiter: List<Ident>,
    metadata: Metadata
) = hentData(hentAlias, identiter)
    .takeIf(::harAvvik)
    ?.let(::genererAvviksMelding)
    ?.let(perioder::hentPerioder)
    ?.also { (_, perioder) ->
        Span.current()
            .setAttribute("perioder", perioder.size.toLong())
            .setAttribute("aktive_perioder", perioder.filter { p -> p.erAktiv }.size.toLong())
    }
    .also { avvikOgPerioder ->
        val lengstAktive = avvikOgPerioder?.perioder?.filter { it.erAktiv }?.minByOrNull { it.fra }
        val sisteStopp = avvikOgPerioder?.perioder?.filter { !it.erAktiv }?.maxByOrNull { it.til ?: Instant.MIN }?.til
        val overlapp = lengstAktive != null &&
                sisteStopp != null &&
                lengstAktive.fra.isBefore(sisteStopp)
        meterRegistry
            .counter(
                "paw_kafka_key_maintenance_aktor_consumer_v2",
                listOf(
                    Tag.of("avvik", (avvikOgPerioder != null).toString()),
                    Tag.of("avslutt_etter_start", overlapp.toString()),
                    Tag.of("perioder", avvikOgPerioder?.perioder?.size.toBucket()),
                    Tag.of("aktive_perioder", avvikOgPerioder?.perioder?.filter { p -> p.erAktiv }?.size.toBucket()),
                    Tag.of("lokale_alias", avvikOgPerioder?.avviksMelding?.lokaleAlias?.size.toBucket()),
                    Tag.of("pdl_identiteter", avvikOgPerioder?.avviksMelding?.pdlIdentitetsnummer?.size.toBucket()),
                    Tag.of(
                        "frie_identer",
                        avvikOgPerioder?.avviksMelding?.lokaleAliasSomIkkeSkalPekePaaPdlPerson()?.size.toBucket()
                    )
                )
            ).increment()
    }
    ?.let(::genererIdOppdatering)
    ?.let(::genererHendelser.partially1(metadata))
    ?: emptyList()

fun Int?.toBucket(): String =
    when {
        this == null -> "NAN"
        this < 10 -> toString()
        else -> "10+"
    }