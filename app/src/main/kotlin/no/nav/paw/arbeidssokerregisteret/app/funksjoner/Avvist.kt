package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist

context(RecordScope<Long>)
fun Tilstand?.avvist(avvist: Avvist): InternTilstandOgApiTilstander =
    when (this?.gjeldeneTilstand) {
        null -> Tilstand(
            recordScope = currentScope(),
            gjeldeneTilstand = GjeldeneTilstand.AVVIST,
            gjeldeneIdentitetsnummer = avvist.identitetsnummer,
            allIdentitetsnummer = setOf(avvist.identitetsnummer),
            gjeldenePeriode = null,
            forrigePeriode = null,
            sisteOpplysningerOmArbeidssoeker = null,
            forrigeOpplysningerOmArbeidssoeker = null
        )

        GjeldeneTilstand.AVVIST -> this.copy(recordScope = currentScope())
        GjeldeneTilstand.STARTET -> this
        GjeldeneTilstand.STOPPET -> copy(
            recordScope = currentScope(),
            gjeldeneTilstand = GjeldeneTilstand.AVVIST
        )
    }.let { nyTilstand: Tilstand ->
        InternTilstandOgApiTilstander(
            tilstand = nyTilstand,
            nyPeriodeTilstand = null,
            nyOpplysningerOmArbeidssoekerTilstand = null
        )
    }
