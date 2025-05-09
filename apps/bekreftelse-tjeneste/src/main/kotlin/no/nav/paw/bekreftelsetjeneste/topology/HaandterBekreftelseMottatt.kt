package no.nav.paw.bekreftelsetjeneste.topology

import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.vo.Bruker
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import no.nav.paw.bekreftelsetjeneste.paavegneav.PaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.paavegneav.Loesning
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.tilstand.*

fun haandterBekreftelseMottatt(
    wallClock: WallClock,
    gjeldendeTilstand: BekreftelseTilstand,
    paaVegneAvTilstand: PaaVegneAvTilstand?,
    melding: no.nav.paw.bekreftelse.melding.v1.Bekreftelse
): Pair<BekreftelseTilstand, List<BekreftelseHendelse>> {
    val (tilstand, hendelser) =
        if (melding.bekreftelsesloesning == Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET) {
            processPawNamespace(wallClock, melding, gjeldendeTilstand, paaVegneAvTilstand)
        } else {
            if (harAnsvar(melding.bekreftelsesloesning, paaVegneAvTilstand)) {
                log(
                    loesning = Loesning.from(melding.bekreftelsesloesning),
                    handling = bekreftelseLevertAction,
                    periodeFunnet = true,
                    harAnsvar = true,
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
                ) to listOfNotNull(
                    melding.svar.vilFortsetteSomArbeidssoeker
                        .takeIf { !it }
                        ?.let {
                            BaOmAaAvsluttePeriode(
                                hendelseId = melding.id,
                                periodeId = melding.periodeId,
                                arbeidssoekerId = gjeldendeTilstand.periode.arbeidsoekerId,
                                hendelseTidspunkt = melding.svar.sendtInnAv.tidspunkt,
                                utfoertAv = Bruker(
                                    type = when (melding.svar.sendtInnAv.utfoertAv.type) {
                                        null -> no.nav.paw.bekreftelse.internehendelser.vo.BrukerType.UDEFINERT
                                        BrukerType.UKJENT_VERDI -> no.nav.paw.bekreftelse.internehendelser.vo.BrukerType.UKJENT_VERDI
                                        BrukerType.UDEFINERT -> no.nav.paw.bekreftelse.internehendelser.vo.BrukerType.UDEFINERT
                                        BrukerType.VEILEDER -> no.nav.paw.bekreftelse.internehendelser.vo.BrukerType.VEILEDER
                                        BrukerType.SYSTEM -> no.nav.paw.bekreftelse.internehendelser.vo.BrukerType.SYSTEM
                                        BrukerType.SLUTTBRUKER -> no.nav.paw.bekreftelse.internehendelser.vo.BrukerType.SLUTTBRUKER
                                    },
                                    id = melding.svar.sendtInnAv.utfoertAv.id,
                                    sikkerhetsnivaa = melding.svar.sendtInnAv.utfoertAv.sikkerhetsnivaa
                                ),
                                kilde = "bekreftelse:${melding.bekreftelsesloesning.name.lowercase()}:${melding.id}"
                            )
                        }
                )
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

fun harAnsvar(
    bekreftelsesloesning: Bekreftelsesloesning,
    paaVegneAvTilstand: PaaVegneAvTilstand?
) = bekreftelsesloesning == Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET ||
        (paaVegneAvTilstand?.paaVegneAvList
            ?: emptyList()).any { it.loesning == Loesning.from(bekreftelsesloesning) }

