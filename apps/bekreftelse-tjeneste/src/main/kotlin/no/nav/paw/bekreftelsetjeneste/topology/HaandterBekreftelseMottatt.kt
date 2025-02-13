package no.nav.paw.bekreftelsetjeneste.topology

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
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
    gjeldendeTilstand: BekreftelseTilstand,
    paaVegneAvTilstand: PaaVegneAvTilstand?,
    melding: no.nav.paw.bekreftelse.melding.v1.Bekreftelse
): Pair<BekreftelseTilstand, List<BekreftelseHendelse>> {
    val harAnsvar = melding.bekreftelsesloesning == Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET ||
            (paaVegneAvTilstand?.paaVegneAvList
                ?: emptyList()).any { it.loesning == Loesning.from(melding.bekreftelsesloesning) }
    val (tilstand, hendelser) =
        if (melding.bekreftelsesloesning == Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET) {
            processPawNamespace(melding, gjeldendeTilstand, paaVegneAvTilstand)
        } else {
            if (harAnsvar) {
                log(
                    loesning = Loesning.from(melding.bekreftelsesloesning),
                    handling = bekreftelseLevertAction,
                    periodeFunnet = true,
                    harAnsvar = true
                )
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
            } else {
                logWarning(
                    Loesning.from(melding.bekreftelsesloesning),
                    bekreftelseLevertAction,
                    Feil.HAR_IKKE_ANSVAR
                )
                gjeldendeTilstand to emptyList()
            }
        }
    return tilstand.copy(
        bekreftelser = tilstand.bekreftelser.filterByStatusAndCount(maksAntallBekreftelserEtterStatus)
    ) to hendelser
}

fun Collection<Bekreftelse>.filterByStatusAndCount(maxSizeConfig: Map<KClass<out BekreftelseTilstandStatus>, Int>): List<Bekreftelse> =
    groupBy { it.sisteTilstand()::class }
        .mapValues { (_, values) -> values.sortedBy { it.gjelderTil }.reversed() }
        .mapValues { (status, values) -> values.take(maxSizeConfig[status] ?: Integer.MAX_VALUE) }
        .flatMap { (_, values) -> values }
        .sortedBy { it.gjelderTil }
