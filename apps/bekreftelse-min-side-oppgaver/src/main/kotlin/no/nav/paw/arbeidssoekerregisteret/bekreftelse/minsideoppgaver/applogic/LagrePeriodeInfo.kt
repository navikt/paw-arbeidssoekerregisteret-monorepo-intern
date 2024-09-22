package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode

fun genererTilstand(
    gjeldeneTilstand: InternTilstand?,
    periode: Periode
): InternTilstand =
    when {
        gjeldeneTilstand == null -> {
            InternTilstand(
                periodeId = periode.id,
                ident = periode.identitetsnummer,
                bekreftelser = emptyList()
            )
        }

        gjeldeneTilstand.ident == periode.identitetsnummer -> gjeldeneTilstand
        else -> gjeldeneTilstand.copy(ident = periode.identitetsnummer)
    }
