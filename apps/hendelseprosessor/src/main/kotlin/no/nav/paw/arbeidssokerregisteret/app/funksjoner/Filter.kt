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
        if (!include) {
            Span.current().addEvent(
                "arbeidssoekerregisteret_ignorert", Attributes.of(
                    AttributeKey.stringKey("arbeidssoekerregisteret_hendelse_type"),
                    hendelse.hendelseType,
                    AttributeKey.stringKey("aarsak"),
                    tilstand?.gjeldeneTilstand?.name ?: "null"
                )
            )
        }
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
            if (!include) {
                Span.current().addEvent(
                    "arbeidssoekerregisteret_ignorert", Attributes.of(
                        AttributeKey.stringKey("arbeidssoekerregisteret_hendelse_type"),
                        hendelse.hendelseType,
                        AttributeKey.stringKey("aarsak"),
                        "ulik_periode_id"
                    )
                )
            }
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
        if (!include) {
            opphoerteIdenterLogger.error(
                "Ignorerte hendelse med opphoert ident: {} {}",
                hendelse.hendelseType,
                hendelse.hendelseId
            )
            Span.current().addEvent(
                "ignorert", Attributes.of(
                    AttributeKey.stringKey("arbeidssoekerregisteret_hendelse_type"),
                    hendelse.hendelseType,
                    AttributeKey.stringKey("aarsak"),
                    tilstand?.gjeldeneTilstand?.name ?: "null"
                )
            )
        }
    }
}