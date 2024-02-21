package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist

context(RecordScope<Long>)
fun TilstandV1?.avvist(avvist: Avvist): InternTilstandOgApiTilstander =
    when (this?.gjeldeneTilstand) {
        null -> TilstandV1(
            recordScope = currentScope(),
            gjeldeneTilstand = GjeldeneTilstand.AVVIST,
            gjeldeneIdentitetsnummer = avvist.identitetsnummer,
            alleIdentitetsnummer = setOf(avvist.identitetsnummer),
            gjeldenePeriode = null,
            forrigePeriode = null,
            sisteOpplysningerOmArbeidssoeker = null,
            forrigeOpplysningerOmArbeidssoeker = null
        )

        GjeldeneTilstand.AVVIST -> this.copy(recordScope = currentScope())
        GjeldeneTilstand.STARTET -> this
        GjeldeneTilstand.AVSLUTTET -> copy(
            recordScope = currentScope(),
            gjeldeneTilstand = GjeldeneTilstand.AVVIST
        )
    }.let { nyTilstand: TilstandV1 ->
        InternTilstandOgApiTilstander(
            tilstand = nyTilstand,
            nyPeriodeTilstand = null,
            nyOpplysningerOmArbeidssoekerTilstand = null
        )
    }
