package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.OppgaveMelding
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet


fun genererOppgaveMeldinger(
    tilstand: InternTilstand,
    hendelse: BekreftelseHendelse?,
    varselMeldingBygger: VarselMeldingBygger
): Pair<InternTilstand?, List<OppgaveMelding>> = when (hendelse) {
    is PeriodeAvsluttet -> {
        null to tilstand.bekreftelser
            .map(varselMeldingBygger::avsluttOppgave)
    }

    is BekreftelseMeldingMottatt -> {
        if (tilstand.bekreftelser.contains(hendelse.bekreftelseId)) {
            tilstand.copy(bekreftelser = tilstand.bekreftelser - hendelse.bekreftelseId) to
                listOf(varselMeldingBygger.avsluttOppgave(hendelse.bekreftelseId))

        } else {
            tilstand to emptyList()
        }
    }

    is BekreftelseTilgjengelig -> {
        tilstand.copy(bekreftelser = tilstand.bekreftelser + hendelse.bekreftelseId) to
                listOf(varselMeldingBygger.opprettOppgave(tilstand.ident, hendelse))

    }

    else -> tilstand to emptyList()
}
