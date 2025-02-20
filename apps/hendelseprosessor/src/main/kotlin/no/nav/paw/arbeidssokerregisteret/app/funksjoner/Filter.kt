@file:OptIn(ExperimentalContracts::class)

package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.app.StreamHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import org.slf4j.LoggerFactory
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun ignorerDuplikatStartOgStopp(
    @Suppress("UNUSED_PARAMETER") recordKey: Long,
    tilstandOgHendelse: InternTilstandOgHendelse
): Boolean {
    val (_, tilstand, hendelse) = tilstandOgHendelse
    return when (tilstand?.gjeldeneTilstand) {
        null -> hendelse.erIkke<Avsluttet>()
        GjeldeneTilstand.STARTET -> hendelse.erIkke<Startet>() || hendelse.erGyldigFeilrettingAvStartTid()
        GjeldeneTilstand.AVSLUTTET -> hendelse.erIkke<Avsluttet>()
        GjeldeneTilstand.AVVIST -> hendelse.erIkke<Avsluttet>()
        else -> false
    }.also { include ->
        if (!include) {
            Span.current().addEvent(
                hendelseIgnorert, Attributes.of(
                    AttributeKey.stringKey("arbeidssoekerregisteret_hendelse_type"),
                    hendelse.hendelseType,
                    aarsakKey,
                    tilstand?.gjeldeneTilstand?.name ?: "null"
                )
            )
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <reified A : StreamHendelse> StreamHendelse.erIkke(): Boolean {
    contract {
        returns(false) implies (this@erIkke is A)
    }
    return this !is A
}

fun Startet.erGyldigFeilrettingAvStartTid(): Boolean =
    metadata.tidspunktFraKilde?.avviksType == AvviksType.TIDSPUNKT_KORRIGERT &&
            metadata.tidspunktFraKilde?.tidspunkt != null

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
                    hendelseIgnorert, Attributes.of(
                        AttributeKey.stringKey("arbeidssoekerregisteret_hendelse_type"),
                        hendelse.hendelseType,
                        aarsakKey,
                        "ulik_periode_id"
                    )
                )
            }
        }
}

private val opphoerteIdenterLogger = LoggerFactory.getLogger("OpphoerteIdenterLogger")

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
                hendelseIgnorert, Attributes.of(
                    AttributeKey.stringKey("arbeidssoekerregisteret_hendelse_type"),
                    hendelse.hendelseType,
                    aarsakKey,
                    tilstand?.gjeldeneTilstand?.name ?: "null"
                )
            )
        }
    }
}
