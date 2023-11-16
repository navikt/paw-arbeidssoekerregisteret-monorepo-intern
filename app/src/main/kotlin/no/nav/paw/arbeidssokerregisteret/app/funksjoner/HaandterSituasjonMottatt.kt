package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.api.v1.Situasjon
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand.STARTET
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.api
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottatt

fun Tilstand?.situasjonMottatt(recordKey: Long, hendelse: SituasjonMottatt): InternTilstandOgApiTilstander =
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
                    sisteSituasjon = hendelse.situasjon,
                    forrigeSituasjon = null
                ),
                nySituasjonTilstand = null,
                nyePeriodeTilstand = null
            )
        }

        this.gjeldenePeriode == null -> {
            InternTilstandOgApiTilstander(
                tilstand = this.copy(
                    sisteSituasjon = hendelse.situasjon,
                    forrigeSituasjon = this.sisteSituasjon
                ),
                nySituasjonTilstand = null,
                nyePeriodeTilstand = null
            )
        }

        else -> {
            InternTilstandOgApiTilstander(
                tilstand = this.copy(
                    sisteSituasjon = hendelse.situasjon,
                    forrigeSituasjon = this.sisteSituasjon
                ),
                nySituasjonTilstand = if (gjeldeneTilstand == STARTET) {
                    Situasjon(
                        hendelse.situasjon.id,
                        gjeldenePeriode.id,
                        hendelse.metadata.api(),
                        hendelse.situasjon.utdanning.api(),
                        hendelse.situasjon.helse.api(),
                        hendelse.situasjon.arbeidserfaring.api(),
                        hendelse.situasjon.arbeidsoekersituasjon.api()
                    )
                } else null,
                nyePeriodeTilstand = null
            )
        }
    }
