package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.app.StreamHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet


@WithSpan(
    value = "filter",
    kind = SpanKind.INTERNAL
)
fun ignorerDuplikatStartOgStopp(
    @Suppress("UNUSED_PARAMETER") recordKey: Long,
    tilstandOgHendelse: InternTilstandOgHendelse
): Boolean {
    val (_, tilstand, hendelse) = tilstandOgHendelse
    return when (tilstand?.gjeldeneTilstand) {
        null -> hendelse.erIkke<Avsluttet>()
        GjeldeneTilstand.STARTET -> hendelse.erIkke<Startet>()
        GjeldeneTilstand.AVSLUTTET -> hendelse.erIkke<Avsluttet>()
        GjeldeneTilstand.AVVIST -> hendelse.erIkke<Avsluttet>()
    }.also { include ->
        val eventName = if (include) "included" else "ignored"
        Span.current().addEvent(
            eventName,
            Attributes.of(
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.type"),
                hendelse.hendelseType,
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.tilstand"),
                tilstand?.gjeldeneTilstand?.name ?: "null"
            )
        )
    }
}

inline fun <reified A : StreamHendelse> StreamHendelse.erIkke(): Boolean = this !is A