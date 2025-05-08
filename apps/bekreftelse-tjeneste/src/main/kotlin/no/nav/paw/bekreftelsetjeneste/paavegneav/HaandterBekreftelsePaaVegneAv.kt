package no.nav.paw.bekreftelsetjeneste.paavegneav

import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.logger
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.GracePeriodeVarselet
import no.nav.paw.bekreftelsetjeneste.tilstand.IkkeKlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.KlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.VenterSvar
import no.nav.paw.bekreftelsetjeneste.tilstand.sisteTilstand
import no.nav.paw.bekreftelsetjeneste.topology.Feil
import no.nav.paw.bekreftelsetjeneste.topology.log
import no.nav.paw.bekreftelsetjeneste.topology.logWarning
import no.nav.paw.bekreftelsetjeneste.topology.paaVegneAvStartet
import no.nav.paw.bekreftelsetjeneste.topology.paaVegneAvStoppet
import java.time.Duration
import java.time.Instant
import java.util.*

@JvmInline
value class WallClock(val value: Instant)

fun haandterBekreftelsePaaVegneAvEndret(
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    wallclock: WallClock,
    bekreftelseTilstand: BekreftelseTilstand?,
    paaVegneAvTilstand: PaaVegneAvTilstand?,
    paaVegneAvHendelse: PaaVegneAv
): List<Handling> {
    return when (val handling = paaVegneAvHendelse.handling) {
        is Start -> startPaaVegneAv(
            wallclock = wallclock,
            bekreftelseTilstand = bekreftelseTilstand,
            paaVegneAvTilstand = paaVegneAvTilstand,
            paaVegneAvHendelse = paaVegneAvHendelse,
            handling = handling
        )

        is Stopp -> haandterStoppPaaVegneAv(
            bekreftelseKonfigurasjon = bekreftelseKonfigurasjon,
            wallclock = wallclock,
            bekreftelseTilstand = bekreftelseTilstand,
            paaVegneAvTilstand = paaVegneAvTilstand,
            paaVegneAvHendelse = paaVegneAvHendelse,
            handling = handling
        )

        else -> emptyList()
    }.also { _ ->
        val action = when (paaVegneAvHendelse.handling) {
            is Start -> paaVegneAvStartet
            is Stopp -> paaVegneAvStoppet
            else -> "ukjent"
        }
        val periodeFunnet = bekreftelseTilstand != null
        val harAnsvar = paaVegneAvTilstand?.paaVegneAvList
            ?.map { it.loesning }
            ?.contains(Loesning.from(paaVegneAvHendelse.bekreftelsesloesning)) ?: false
        if (!periodeFunnet) {
            logWarning(
                loesning = Loesning.from(paaVegneAvHendelse.bekreftelsesloesning),
                handling = action,
                feil = Feil.PERIODE_IKKE_FUNNET,
                harAnsvar = harAnsvar,
                fristBrutt = (paaVegneAvHendelse.handling as? Stopp)?.fristBrutt
            )
        } else {
            log(
                loesning = Loesning.from(paaVegneAvHendelse.bekreftelsesloesning),
                handling = action,
                periodeFunnet = periodeFunnet,
                harAnsvar = harAnsvar,
                fristBrutt = (paaVegneAvHendelse.handling as? Stopp)?.fristBrutt
            )
        }
    }
}

fun startPaaVegneAv(
    wallclock: WallClock,
    bekreftelseTilstand: BekreftelseTilstand?,
    paaVegneAvTilstand: PaaVegneAvTilstand?,
    paaVegneAvHendelse: PaaVegneAv,
    handling: Start
): List<Handling> {
    val oppdatertPaaVegneAvTilstand =
        (paaVegneAvTilstand ?: opprettPaaVegneAvTilstand(paaVegneAvHendelse.periodeId)) +
                InternPaaVegneAv(
                    loesning = Loesning.from(paaVegneAvHendelse.bekreftelsesloesning),
                    intervall = Duration.ofMillis(handling.intervalMS),
                    gracePeriode = Duration.ofMillis(handling.graceMS)
                )
    val hendelse = bekreftelseTilstand?.let {
        BekreftelsePaaVegneAvStartet(
            hendelseId = UUID.randomUUID(),
            periodeId = paaVegneAvHendelse.periodeId,
            arbeidssoekerId = bekreftelseTilstand.periode.arbeidsoekerId,
            hendelseTidspunkt = wallclock.value,
        )
    }

    val oppdaterBekreftelseTilstand = bekreftelseTilstand?.let {
        val oppdaterteBekreftelser = it.bekreftelser
            .mapNotNull { bekreftelse ->
                when (bekreftelse.sisteTilstand()) {
                    is VenterSvar,
                    is KlarForUtfylling,
                    is GracePeriodeVarselet,
                    is IkkeKlarForUtfylling -> {
                        logger.info("Mottatt start på vegne av, sletter bekreftelse: ${bekreftelse.bekreftelseId}")
                        null
                    }

                    else -> bekreftelse
                }
            }
        it.copy(bekreftelser = oppdaterteBekreftelser)
    }
        ?.takeIf { oppdatertBekreftelseTilstand -> oppdatertBekreftelseTilstand != bekreftelseTilstand }
        ?.let { oppdatertBekreftelseTilstand ->
            SkrivBekreftelseTilstand(
                oppdatertBekreftelseTilstand.periode.periodeId,
                oppdatertBekreftelseTilstand
            )
        }

    return listOfNotNull(
        if (paaVegneAvTilstand != oppdatertPaaVegneAvTilstand) SkrivPaaVegneAvTilstand(
            paaVegneAvHendelse.periodeId,
            oppdatertPaaVegneAvTilstand
        ) else null,
        oppdaterBekreftelseTilstand,
        hendelse?.let(::SendHendelse)
    )
}


sealed interface Handling
data class SlettPaaVegneAvTilstand(val id: UUID) : Handling
data class SkrivPaaVegneAvTilstand(val id: UUID, val value: PaaVegneAvTilstand) : Handling
data class SkrivBekreftelseTilstand(val id: UUID, val value: BekreftelseTilstand) : Handling
data class SendHendelse(val hendelse: BekreftelseHendelse) : Handling
