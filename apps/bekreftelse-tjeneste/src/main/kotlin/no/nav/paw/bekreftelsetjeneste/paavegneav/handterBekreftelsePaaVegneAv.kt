package no.nav.paw.bekreftelsetjeneste.paavegneav

import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import java.time.Duration
import java.time.Instant
import java.util.*

@JvmInline
value class WallClock(val value: Instant)

fun haandterBekreftelsePaaVegneAvEndret(
    wallclock: WallClock,
    tilstand: InternTilstand?,
    paaVegneAvTilstand: PaaVegneAvTilstand?,
    paaVegneAv: PaaVegneAv
): List<Handling> {
    return when (val handling = paaVegneAv.handling) {
        is Start -> startPaaVegneAv(
            wallclock = wallclock,
            tilstand = tilstand,
            paaVegneAvTilstand = paaVegneAvTilstand,
            paaVegneAv = paaVegneAv,
            handling = handling
        )

        is Stopp -> stoppPaaVegneAv(
            paaVegneAvTilstand = paaVegneAvTilstand,
            paaVegneAv = paaVegneAv
        )

        else -> emptyList()
    }
}

fun stoppPaaVegneAv(
    paaVegneAvTilstand: PaaVegneAvTilstand?,
    paaVegneAv: PaaVegneAv
): List<Handling> {
    val oppdatertPaaVegneAv = paaVegneAvTilstand - Loesning.from(paaVegneAv.bekreftelsesloesning)
    val paaVegneAvHandling = when {
        paaVegneAvTilstand != null && oppdatertPaaVegneAv == null -> SlettBekreftelsePaaVegneAv(paaVegneAv.periodeId)
        paaVegneAvTilstand != null && oppdatertPaaVegneAv != null -> SkrivBekreftelsePaaVegneAv(paaVegneAv.periodeId, oppdatertPaaVegneAv)
        else -> null
    }
    return listOfNotNull(paaVegneAvHandling)
}

fun startPaaVegneAv(
    wallclock: WallClock,
    tilstand: InternTilstand?,
    paaVegneAvTilstand: PaaVegneAvTilstand?,
    paaVegneAv: PaaVegneAv,
    handling: Start
): List<Handling> {
    val oppdatertInternPaaVegneAv =
        (paaVegneAvTilstand ?: bekreftelsePaaVegneAvTilstand(paaVegneAv.periodeId)) +
                InternPaaVegneAv(
                    loesning = Loesning.from(paaVegneAv.bekreftelsesloesning),
                    intervall = Duration.ofMillis(handling.intervalMS),
                    gracePeriode = Duration.ofMillis(handling.graceMS)
                )
    val hendelse = tilstand?.let {
        BekreftelsePaaVegneAvStartet(
            hendelseId = UUID.randomUUID(),
            periodeId = paaVegneAv.periodeId,
            arbeidssoekerId = tilstand.periode.arbeidsoekerId,
            hendelseTidspunkt = wallclock.value,
        )
    }
    val oppdaterInternTilstand = tilstand?.let {
        val oppdaterteBekreftelser = it.bekreftelser
            .map { bekreftelse ->
                when (bekreftelse.sisteTilstand()) {
                    is VenterSvar,
                    is KlarForUtfylling,
                    is GracePeriodeVarselet,
                    is IkkeKlarForUtfylling -> bekreftelse + InternBekreftelsePaaVegneAvStartet(wallclock.value)
                    else -> bekreftelse
                }
            }
        it.copy(bekreftelser = oppdaterteBekreftelser)
    }
        ?.takeIf { oppdatertTilstand -> oppdatertTilstand != tilstand }
        ?.let { oppdatertTilstand -> SkrivInternTilstand(oppdatertTilstand.periode.periodeId, oppdatertTilstand) }

    return listOfNotNull(
        if (paaVegneAvTilstand != oppdatertInternPaaVegneAv) SkrivBekreftelsePaaVegneAv(paaVegneAv.periodeId, oppdatertInternPaaVegneAv) else null,
        oppdaterInternTilstand,
        hendelse?.let(::SendHendelse)
    )
}


sealed interface Handling
data class SlettBekreftelsePaaVegneAv(val id: UUID) : Handling
data class SkrivBekreftelsePaaVegneAv(val id: UUID, val value: PaaVegneAvTilstand) : Handling
data class SkrivInternTilstand(val id: UUID, val value: InternTilstand) : Handling
data class SendHendelse(val hendelse: BekreftelseHendelse) : Handling
