package no.nav.paw.bekreftelsetjeneste.topology

import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelsetjeneste.paavegneav.PaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.paavegneav.Loesning
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import kotlin.reflect.KClass

val maksAntallBekreftelserEtterStatus = mapOf(
    Levert::class to 1,
    InternBekreftelsePaaVegneAvStartet::class to 4,
    GracePeriodeUtloept::class to 10
)

fun haandterBekreftelseMottatt(
    gjeldendeTilstand: InternTilstand,
    paaVegneAvTilstand: PaaVegneAvTilstand?,
    melding: no.nav.paw.bekreftelse.melding.v1.Bekreftelse
): Pair<InternTilstand, List<BekreftelseHendelse>> {
    val (tilstand, hendelser) = if (melding.bekreftelsesloesning == Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET) {
        processPawNamespace(melding, gjeldendeTilstand)
    } else {
        val paaVegneAvList = paaVegneAvTilstand?.internPaaVegneAvList ?: emptyList()
        if (paaVegneAvList.any { it.loesning == Loesning.from(melding.bekreftelsesloesning) }) {
            gjeldendeTilstand.leggTilNyEllerOppdaterBekreftelse(
                Bekreftelse(
                    tilstandsLogg = BekreftelseTilstandsLogg(
                        siste = Levert(melding.svar.sendtInnAv.tidspunkt),
                        tidligere = emptyList()
                    ),
                    bekreftelseId = melding.id,
                    gjelderFra = melding.svar.gjelderFra,
                    gjelderTil = melding.svar.gjelderTil
                )
            ) to emptyList()
        } else gjeldendeTilstand to emptyList()
    }
    return tilstand.copy(
        bekreftelser = tilstand.bekreftelser.filterByStatusAndCount(maksAntallBekreftelserEtterStatus)
    ) to hendelser
}

fun Collection<Bekreftelse>.filterByStatusAndCount(maxSizeConfig: Map<KClass<out BekreftelseTilstand>, Int>): List<Bekreftelse> =
    groupBy { it.sisteTilstand()::class }
        .mapValues { (_, values) -> values.sortedBy { it.gjelderTil }.reversed() }
        .mapValues { (status, values) -> values.take(maxSizeConfig[status] ?: Integer.MAX_VALUE) }
        .flatMap { (_, values) -> values }
        .sortedBy { it.gjelderTil }
