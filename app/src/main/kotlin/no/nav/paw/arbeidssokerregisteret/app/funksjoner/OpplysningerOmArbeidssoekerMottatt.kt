package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand.STARTET
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1
import no.nav.paw.arbeidssokerregisteret.app.tilstand.api
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt

context(HendelseScope<Long>)
fun TilstandV1?.opplysningerOmArbeidssoekerMottatt(hendelse: OpplysningerOmArbeidssoekerMottatt): InternTilstandOgApiTilstander =
    when {
        this == null -> {
            InternTilstandOgApiTilstander(
                id = id,
                tilstand = TilstandV1(
                    hendelseScope = currentScope(),
                    gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
                    alleIdentitetsnummer = setOf(hendelse.identitetsnummer),
                    gjeldeneTilstand = GjeldeneTilstand.AVSLUTTET,
                    gjeldenePeriode = null,
                    forrigePeriode = null,
                    sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
                    forrigeOpplysningerOmArbeidssoeker = null
                ),
                nyOpplysningerOmArbeidssoekerTilstand = null,
                nyPeriodeTilstand = null
            )
        }

        this.gjeldenePeriode == null -> {
            InternTilstandOgApiTilstander(
                id = id,
                tilstand = this.copy(
                    sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
                    forrigeOpplysningerOmArbeidssoeker = this.sisteOpplysningerOmArbeidssoeker,
                    gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
                    alleIdentitetsnummer = this.alleIdentitetsnummer + hendelse.identitetsnummer,
                    hendelseScope = currentScope()
                ),
                nyOpplysningerOmArbeidssoekerTilstand = null,
                nyPeriodeTilstand = null
            )
        }

        else -> {
            InternTilstandOgApiTilstander(
                id = id,
                tilstand = this.copy(
                    sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
                    forrigeOpplysningerOmArbeidssoeker = this.sisteOpplysningerOmArbeidssoeker,
                    hendelseScope = currentScope()
                ),
                nyOpplysningerOmArbeidssoekerTilstand = if (gjeldeneTilstand == STARTET) {
                    OpplysningerOmArbeidssoeker(
                        hendelse.opplysningerOmArbeidssoeker.id,
                        gjeldenePeriode.id,
                        hendelse.metadata.api(),
                        hendelse.opplysningerOmArbeidssoeker.utdanning?.api(),
                        hendelse.opplysningerOmArbeidssoeker.helse?.api(),
                        hendelse.opplysningerOmArbeidssoeker.jobbsituasjon.api(),
                        hendelse.opplysningerOmArbeidssoeker.annet?.api()
                    )
                } else null,
                nyPeriodeTilstand = null
            )
        }
    }
