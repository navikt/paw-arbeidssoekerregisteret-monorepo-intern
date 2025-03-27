package no.nav.paw.bekreftelsetjeneste.paavegneav

import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloeptEtterEksternInnsamling
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstandsLogg
import no.nav.paw.bekreftelsetjeneste.tilstand.Levert
import no.nav.paw.bekreftelsetjeneste.tilstand.leggTilNyEllerOppdaterBekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.sisteTilstand
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

private val stoppPaaVegneAvLogger = LoggerFactory.getLogger("stopp_paa_vegne_av")
fun haandterStoppPaaVegneAv(
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    wallclock: WallClock,
    bekreftelseTilstand: BekreftelseTilstand?,
    paaVegneAvTilstand: PaaVegneAvTilstand?,
    paaVegneAvHendelse: PaaVegneAv,
    handling: Stopp
): List<Handling> {
    val ansvar = paaVegneAvTilstand?.ansvar(Loesning.from(paaVegneAvHendelse.bekreftelsesloesning))
    return when {
        bekreftelseTilstand == null -> emptyList()
        paaVegneAvTilstand != null && ansvar != null -> {
            stoppPaaVeieneAv(
                ansvar = ansvar,
                bekreftelseKonfigurasjon = bekreftelseKonfigurasjon,
                handling = handling,
                paaVegneAvHendelse = paaVegneAvHendelse,
                bekreftelseTilstand = bekreftelseTilstand,
                wallclock = wallclock,
                paaVegneAvTilstand = paaVegneAvTilstand
            )
        }
        else -> {
            emptyList()
        }
    }.also { resultat ->
        val hendelse =
            resultat.firstOrNull { it is SendHendelse && it.hendelse is RegisterGracePeriodeUtloeptEtterEksternInnsamling }
        val fraTilDummy = bekreftelseTilstand?.bekreftelser
            ?.filter { it.sisteTilstand() is Levert }
            ?.maxByOrNull { it.gjelderTil }
            ?.let { Triple(it.gjelderFra, it.gjelderTil, it.dummy) }

        stoppPaaVegneAvLogger.trace(
            "hadde_ansvar: {}, sist_leverte: [{} -> {}], er_dummy: {}, avslutt_periode: {}, loesning: {}, periode_startet: {}",
            ansvar != null,
            fraTilDummy?.first,
            fraTilDummy?.second,
            fraTilDummy?.third ?: false,
            hendelse != null,
            paaVegneAvHendelse.bekreftelsesloesning.name,
            bekreftelseTilstand?.periode?.startet
        )
    }
}

private fun stoppPaaVeieneAv(
    ansvar: InternPaaVegneAv,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    handling: Stopp,
    paaVegneAvHendelse: PaaVegneAv,
    bekreftelseTilstand: BekreftelseTilstand,
    wallclock: WallClock,
    paaVegneAvTilstand: PaaVegneAvTilstand
): List<Handling> {
    val kanStoppe =
        (ansvar.intervall + ansvar.gracePeriode) >= (bekreftelseKonfigurasjon.interval + bekreftelseKonfigurasjon.graceperiode)
    val utgaaendeHandling: Handling? = if (kanStoppe && handling.fristBrutt) {
        SendHendelse(
            RegisterGracePeriodeUtloeptEtterEksternInnsamling(
                hendelseId = UUID.randomUUID(),
                periodeId = paaVegneAvHendelse.periodeId,
                arbeidssoekerId = bekreftelseTilstand.periode.arbeidsoekerId,
                hendelseTidspunkt = wallclock.value
            )
        )
    } else {
        val sistLeverte = bekreftelseTilstand.bekreftelser
            .filter { it.sisteTilstand() is Levert }
            .maxByOrNull { it.gjelderFra }
        val potensiellStartDato = sistLeverte?.gjelderTil ?: bekreftelseTilstand.periode.startet
        if (Duration.between(potensiellStartDato, wallclock.value) > bekreftelseKonfigurasjon.interval.dividedBy(2)) {
            SkrivBekreftelseTilstand(
                bekreftelseTilstand.periode.periodeId,
                bekreftelseTilstand.leggTilNyEllerOppdaterBekreftelse(
                    ny = Bekreftelse(
                        tilstandsLogg = BekreftelseTilstandsLogg(Levert(wallclock.value), emptyList()),
                        bekreftelseId = UUID.randomUUID(),
                        gjelderFra = wallclock.value - Duration.ofDays(1),
                        gjelderTil = wallclock.value,
                        dummy = true
                    )
                )
            )
        } else  null
    }
    val ansvarsHandling = fjaernLoesningFraAnsvarsliste(paaVegneAvTilstand, ansvar.loesning)
    return listOfNotNull(utgaaendeHandling, ansvarsHandling)
}

fun fjaernLoesningFraAnsvarsliste(
    paaVegneAvTilstand: PaaVegneAvTilstand,
    loesning: Loesning,
): Handling {
    val oppdatertPaaVegneAv = paaVegneAvTilstand - loesning
    val paaVegneAvHandling = when {
        oppdatertPaaVegneAv == null -> SlettPaaVegneAvTilstand(paaVegneAvTilstand.periodeId)
        else -> SkrivPaaVegneAvTilstand(
            paaVegneAvTilstand.periodeId,
            oppdatertPaaVegneAv
        )
    }
    return paaVegneAvHandling
}

fun PaaVegneAvTilstand.ansvar(loesning: Loesning): InternPaaVegneAv? {
    return this.paaVegneAvList.firstOrNull { it.loesning == loesning }
}