package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.api.v1.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand.STARTET
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.api
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt

context(RecordScope<Long>)
fun Tilstand?.opplysningerOmArbeidssoekerMottatt(hendelse: OpplysningerOmArbeidssoekerMottatt): InternTilstandOgApiTilstander =
    when {
        this == null -> {
            InternTilstandOgApiTilstander(
                recordScope = currentScope(),
                tilstand = Tilstand(
                    recordScope = currentScope(),
                    gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
                    allIdentitetsnummer = setOf(hendelse.identitetsnummer),
                    gjeldeneTilstand = GjeldeneTilstand.STOPPET,
                    gjeldenePeriode = null,
                    forrigePeriode = null,
                    sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
                    forrigeOpplysningerOmArbeidssoeker = null
                ),
                nyOpplysningerOmArbeidssoekerTilstand = null,
                nyePeriodeTilstand = null
            )
        }

        this.gjeldenePeriode == null -> {
            InternTilstandOgApiTilstander(
                recordScope = currentScope(),
                tilstand = this.copy(
                    sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
                    forrigeOpplysningerOmArbeidssoeker = this.sisteOpplysningerOmArbeidssoeker,
                    recordScope = currentScope()
                ),
                nyOpplysningerOmArbeidssoekerTilstand = null,
                nyePeriodeTilstand = null
            )
        }

        else -> {
            InternTilstandOgApiTilstander(
                recordScope = currentScope(),
                tilstand = this.copy(
                    sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
                    forrigeOpplysningerOmArbeidssoeker = this.sisteOpplysningerOmArbeidssoeker,
                    recordScope = currentScope()
                ),
                nyOpplysningerOmArbeidssoekerTilstand = if (gjeldeneTilstand == STARTET) {
                    OpplysningerOmArbeidssoeker(
                        hendelse.opplysningerOmArbeidssoeker.id,
                        gjeldenePeriode.id,
                        hendelse.metadata.api(),
                        hendelse.opplysningerOmArbeidssoeker.utdanning.api(),
                        hendelse.opplysningerOmArbeidssoeker.helse.api(),
                        hendelse.opplysningerOmArbeidssoeker.arbeidserfaring.api(),
                        hendelse.opplysningerOmArbeidssoeker.jobbsituasjon.api(),
                        hendelse.opplysningerOmArbeidssoeker.annet.api()
                    )
                } else null,
                nyePeriodeTilstand = null
            )
        }
    }
