package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.OppgaveMelding
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet

fun Periode.asInternTilstand(
    gjeldeneTilstand: InternTilstand?,
): InternTilstand =
    when {
        gjeldeneTilstand == null -> {
            InternTilstand(
                periodeId = id,
                ident = identitetsnummer,
                bekreftelser = emptyList()
            )
        }

        gjeldeneTilstand.ident == identitetsnummer -> gjeldeneTilstand
        else -> gjeldeneTilstand.copy(ident = identitetsnummer)
    }

fun InternTilstand.asOppgaveMeldinger(
    hendelse: BekreftelseHendelse?,
    varselMeldingBygger: VarselMeldingBygger
): Pair<InternTilstand?, List<OppgaveMelding>> =
    when (hendelse) {
        is PeriodeAvsluttet -> {
            null to bekreftelser
                .map(varselMeldingBygger::avsluttOppgave)
        }

        is BekreftelseMeldingMottatt -> {
            if (bekreftelser.contains(hendelse.bekreftelseId)) {
                this.copy(bekreftelser = bekreftelser - hendelse.bekreftelseId) to
                        listOf(varselMeldingBygger.avsluttOppgave(hendelse.bekreftelseId))

            } else {
                this to emptyList()
            }
        }

        is BekreftelseTilgjengelig -> {
            this.copy(bekreftelser = bekreftelser + hendelse.bekreftelseId) to
                    listOf(varselMeldingBygger.opprettOppgave(ident, hendelse))

        }

        else -> this to emptyList()
    }
