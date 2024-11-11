package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist


fun FunctionContext<TilstandV1?, Long>.avvist(avvist: Avvist): InternTilstandOgApiTilstander =
    when (tilstand?.gjeldeneTilstand) {
        null -> TilstandV1(
            hendelseScope = scope,
            gjeldeneTilstand = GjeldeneTilstand.AVVIST,
            gjeldeneIdentitetsnummer = avvist.identitetsnummer,
            alleIdentitetsnummer = setOf(avvist.identitetsnummer),
            gjeldenePeriode = null,
            forrigePeriode = null,
            sisteOpplysningerOmArbeidssoeker = null,
            forrigeOpplysningerOmArbeidssoeker = null
        )

        GjeldeneTilstand.AVVIST -> tilstand.copy(hendelseScope = scope)
        GjeldeneTilstand.STARTET, GjeldeneTilstand.OPPHOERT -> tilstand
        GjeldeneTilstand.AVSLUTTET -> tilstand.copy(
            hendelseScope = scope,
            gjeldeneTilstand = GjeldeneTilstand.AVVIST
        )
    }.let { nyTilstand: TilstandV1 ->
        InternTilstandOgApiTilstander(
            id = scope.id,
            tilstand = nyTilstand,
            nyPeriodeTilstand = null,
            nyOpplysningerOmArbeidssoekerTilstand = null
        )
    }
