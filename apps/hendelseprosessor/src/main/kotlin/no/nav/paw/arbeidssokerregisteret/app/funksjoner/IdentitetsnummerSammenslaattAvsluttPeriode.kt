package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1
import no.nav.paw.arbeidssokerregisteret.app.tilstand.api
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import org.slf4j.LoggerFactory

private val identitetsnummerSammenslaattLogger = LoggerFactory.getLogger("identitetsnummerSammenslaattAvsluttPeriode")

fun FunctionContext<TilstandV1?, Long>.identitetsnummerSammenslaattAvsluttPeriode(hendelse: IdentitetsnummerSammenslaatt): InternTilstandOgApiTilstander {
    return if (tilstand?.gjeldenePeriode == null) {
        identitetsnummerSammenslaattLogger.info("Gjeldende periode er null, kan ikke avslutte periode for sammenslått identitetsnummer. Ignorerer hendelse: ${hendelse.hendelseId}")
        InternTilstandOgApiTilstander(
            id = scope.id,
            tilstand = tilstand?.copy(
                gjeldeneTilstand = GjeldeneTilstand.OPPHOERT,
            ) ?: TilstandV1(
                hendelseScope = scope,
                gjeldeneTilstand = GjeldeneTilstand.OPPHOERT,
                gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
                alleIdentitetsnummer = setOf(hendelse.identitetsnummer),
                gjeldenePeriode = null,
                forrigePeriode = null,
                sisteOpplysningerOmArbeidssoeker = null,
                forrigeOpplysningerOmArbeidssoeker = null
            ),
            nyPeriodeTilstand = null,
            nyOpplysningerOmArbeidssoekerTilstand = null
        )
    } else {
        val stoppetPeriode = tilstand.gjeldenePeriode.copy(
            avsluttet = hendelse.metadata,
            avsluttetVedOffset = scope.offset
        )
        InternTilstandOgApiTilstander(
            id = scope.id,
            tilstand = tilstand.copy(
                gjeldeneTilstand = GjeldeneTilstand.OPPHOERT,
                gjeldenePeriode = null,
                forrigePeriode = stoppetPeriode,
                gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
                alleIdentitetsnummer = (tilstand.alleIdentitetsnummer + hendelse.flyttedeIdentitetsnumre + hendelse.identitetsnummer).toSet(),
                hendelseScope = scope
            ),
            nyPeriodeTilstand = Periode(
                stoppetPeriode.id,
                stoppetPeriode.identitetsnummer,
                stoppetPeriode.startet.api(),
                stoppetPeriode.avsluttet?.api()
            ),
            nyOpplysningerOmArbeidssoekerTilstand = null
        )
    }
}