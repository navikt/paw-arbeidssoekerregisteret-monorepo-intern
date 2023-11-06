package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.api
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.situasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottat

fun Tilstand?.situasjonMottatt(recordKey: Long, hendelse: SituasjonMottat): InternTilstandOgApiTilstander =
    when {
        this == null -> {
            InternTilstandOgApiTilstander(
                tilstand = Tilstand(
                    kafkaKey = recordKey,
                    gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
                    allIdentitetsnummer = setOf(hendelse.identitetsnummer),
                    gjeldeneTilstand = GjeldeneTilstand.STOPPET,
                    gjeldenePeriode = null,
                    forrigePeriode = null,
                    sisteSituasjon = situasjon(hendelse),
                    forrigeSituasjon = null
                ),
                situasjon = null,
                periode = null
            )
        }

        this.gjeldenePeriode == null -> {
            InternTilstandOgApiTilstander(
                tilstand = this.copy(
                    sisteSituasjon = situasjon(hendelse),
                    forrigeSituasjon = this.sisteSituasjon
                ),
                situasjon = null,
                periode = null
            )
        }

        else -> {
            InternTilstandOgApiTilstander(
                tilstand = this.copy(
                    sisteSituasjon = situasjon(hendelse),
                    forrigeSituasjon = this.sisteSituasjon
                ),
                situasjon = situasjon(hendelse).api(this.gjeldenePeriode.id),
                periode = null
            )
        }
    }
