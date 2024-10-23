package no.nav.paw.bekreftelsetjeneste.ansvar

import no.nav.paw.bekreftelse.ansvar.v1.AnsvarEndret
import no.nav.paw.bekreftelse.ansvar.v1.vo.AvslutterAnsvar
import no.nav.paw.bekreftelse.ansvar.v1.vo.TarAnsvar
import no.nav.paw.bekreftelse.internehendelser.AndreHarOvertattAnsvar
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import java.time.Duration
import java.time.Instant
import java.util.*

@JvmInline
value class WallClock(val value: Instant)

fun haandterAnsvarEndret(
    wallclock: WallClock,
    tilstand: InternTilstand?,
    ansvar: Ansvar?,
    ansvarEndret: AnsvarEndret
): List<Handling> {
    return when (val handling = ansvarEndret.handling) {
        is TarAnsvar -> tarAnsvar(
            wallclock = wallclock,
            tilstand = tilstand,
            ansvar = ansvar,
            ansvarEndret = ansvarEndret,
            handling = handling
        )

        is AvslutterAnsvar -> avslutterAnsvar(
            ansvar = ansvar,
            ansvarEndret = ansvarEndret
        )

        else -> emptyList()
    }
}

fun avslutterAnsvar(
    ansvar: Ansvar?,
    ansvarEndret: AnsvarEndret
): List<Handling> {
    val oppdatertAnsvar = ansvar - Loesning.from(ansvarEndret.bekreftelsesloesning)
    val ansvarsHandling = when {
        ansvar != null && oppdatertAnsvar == null -> SlettAnsvar(ansvarEndret.periodeId)
        ansvar != null && oppdatertAnsvar != null -> SkrivAnsvar(ansvarEndret.periodeId, oppdatertAnsvar)
        else -> null
    }
    return listOfNotNull(ansvarsHandling)
}

fun tarAnsvar(
    wallclock: WallClock,
    tilstand: InternTilstand?,
    ansvar: Ansvar?,
    ansvarEndret: AnsvarEndret,
    handling: TarAnsvar
): List<Handling> {
    val oppdatertAnsvar =
        (ansvar ?: ansvar(ansvarEndret.periodeId)) +
                Ansvarlig(
                    loesning = Loesning.from(ansvarEndret.bekreftelsesloesning),
                    intervall = Duration.ofMillis(handling.intervalMS),
                    gracePeriode = Duration.ofMillis(handling.graceMS)
                )
    val hendelse = tilstand?.let {
        AndreHarOvertattAnsvar(
            hendelseId = UUID.randomUUID(),
            periodeId = ansvarEndret.periodeId,
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
                    is IkkeKlarForUtfylling -> bekreftelse + AnsvarOvertattAvAndre(wallclock.value)
                    else -> bekreftelse
                }
            }
        it.copy(bekreftelser = oppdaterteBekreftelser)
    }
        ?.takeIf { oppdatertTilstand -> oppdatertTilstand != tilstand }
        ?.let { oppdatertTilstand -> SkrivInternTilstand(oppdatertTilstand.periode.periodeId, oppdatertTilstand) }

    return listOfNotNull(
        if (ansvar != oppdatertAnsvar) SkrivAnsvar(ansvarEndret.periodeId, oppdatertAnsvar) else null,
        oppdaterInternTilstand,
        hendelse?.let(::SendHendelse)
    )
}


sealed interface Handling
data class SlettAnsvar(val id: UUID) : Handling
data class SkrivAnsvar(val id: UUID, val value: Ansvar) : Handling
data class SkrivInternTilstand(val id: UUID, val value: InternTilstand) : Handling
data class SendHendelse(val hendelse: BekreftelseHendelse) : Handling
