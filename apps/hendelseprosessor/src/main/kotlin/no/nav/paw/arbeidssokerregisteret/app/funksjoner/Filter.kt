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
import org.slf4j.LoggerFactory

@WithSpan(
    value = "ignorerDuplikatStartOgStopp",
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
        else -> false
    }.also { include ->
        Span.current().setAllAttributes(
            Attributes.of(
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.type"),
                hendelse.hendelseType,
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.inkludert"),
                include.toString(),
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.tilstand"),
                tilstand?.gjeldeneTilstand?.name ?: "null"
            )
        )
    }
}

inline fun <reified A : StreamHendelse> StreamHendelse.erIkke(): Boolean = this !is A

@WithSpan(
    value = "ignorerAvsluttetForAnnenPeriode",
    kind = SpanKind.INTERNAL
)
fun ignorerAvsluttetForAnnenPeriode(
    @Suppress("UNUSED_PARAMETER") recordKey: Long,
    tilstandOgHendelse: InternTilstandOgHendelse
): Boolean {
    val (_, tilstand, hendelse) = tilstandOgHendelse
    if (hendelse !is Avsluttet) return true
    val gjeldenePeriodeId = tilstand?.gjeldenePeriode?.id
    return (((gjeldenePeriodeId == null) || (hendelse.periodeId == null) || (gjeldenePeriodeId == hendelse.periodeId)))
        .also { include ->
            Span.current().setAllAttributes(
                Attributes.of(
                    AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.type"),
                    hendelse.hendelseType,
                    AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.periodeIdErSatt"),
                    (hendelse as? Avsluttet)?.let { it.periodeId != null }?.toString() ?: "NA",
                    AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.inkludert"),
                    include.toString(),
                    AttributeKey.stringKey("paw.arbeidssoekerregisteret.tilstand.periodeIdErSatt"),
                    (tilstandOgHendelse.tilstand?.gjeldenePeriode?.id != null).toString()
                )
            )
        }
}

private val opphoerteIdenterLogger = LoggerFactory.getLogger("OpphoerteIdenterLogger")

@WithSpan(
    value = "ignorerOpphoerteIdenter",
    kind = SpanKind.INTERNAL
)
fun ignorerOpphoerteIdenter(
    @Suppress("UNUSED_PARAMETER") recordKey: Long,
    tilstandOgHendelse: InternTilstandOgHendelse
): Boolean {
    val (_, tilstand, hendelse) = tilstandOgHendelse
    return (tilstand?.gjeldeneTilstand != GjeldeneTilstand.OPPHOERT).also { include ->
        if(!include) {
            opphoerteIdenterLogger.error("Ignorerte opphoert ident for hendelse: {} {}", hendelse.hendelseType, hendelse.hendelseId)
        }
        Span.current().setAllAttributes(
            Attributes.of(
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.type"),
                hendelse.hendelseType,
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.inkludert"),
                include.toString(),
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.tilstand"),
                tilstand?.gjeldeneTilstand?.name ?: "null",
            )
        )
    }
}