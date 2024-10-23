package no.nav.paw.bekreftelsetjeneste.topology

import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelsetjeneste.ansvar.Ansvar
import no.nav.paw.bekreftelsetjeneste.ansvar.Loesning
import no.nav.paw.bekreftelsetjeneste.tilstand.*

fun haandterBekreftelseMottatt(gjeldendeTilstand: InternTilstand, ansvar: Ansvar?, melding: no.nav.paw.bekreftelse.melding.v1.Bekreftelse): Pair<InternTilstand, List<BekreftelseHendelse>> {
    return if (melding.bekreftelsesloesning == Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET) {
        processPawNamespace(melding, gjeldendeTilstand)
    } else {
        val ansvarlige = ansvar?.ansvarlige ?: emptyList()
        if (ansvarlige.any { it.loesning == Loesning.from(melding.bekreftelsesloesning) }) {
            gjeldendeTilstand.oppdaterBekreftelse(
                Bekreftelse(
                    tilstandsLogg = BekreftelseTilstandsLogg(
                        siste = Levert(melding.svar.sendtInn.tidspunkt),
                        tidligere = emptyList()
                    ),
                    bekreftelseId = melding.id,
                    gjelderFra = melding.svar.gjelderFra,
                    gjelderTil = melding.svar.gjelderTil
                )
            ) to emptyList()
        } else gjeldendeTilstand to emptyList()
    }
}