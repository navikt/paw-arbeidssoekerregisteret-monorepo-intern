package no.nav.paw.bekreftelsetjeneste.topology

import arrow.core.andThen
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.metrics.tellBekreftelseHandling
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstandStatus
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstandsLogg
import no.nav.paw.bekreftelsetjeneste.tilstand.GracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.tilstand.GracePeriodeVarselet
import no.nav.paw.bekreftelsetjeneste.tilstand.IkkeKlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.KlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.VenterPaaSvar
import no.nav.paw.bekreftelsetjeneste.tilstand.VenterSvar
import no.nav.paw.bekreftelsetjeneste.tilstand.erKlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.erSisteVarselOmGjenstaaendeGraceTid
import no.nav.paw.bekreftelsetjeneste.tilstand.gjenstaendeGraceperiode
import no.nav.paw.bekreftelsetjeneste.tilstand.harFristUtloept
import no.nav.paw.bekreftelsetjeneste.tilstand.harGraceperiodeUtloept
import no.nav.paw.bekreftelsetjeneste.tilstand.plus
import no.nav.paw.bekreftelsetjeneste.tilstand.sisteTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.sluttTidForBekreftelsePeriode
import no.nav.paw.collections.PawNonEmptyList
import no.nav.paw.collections.pawNonEmptyListOf
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import java.util.*

fun BekreftelseContext.prosesser(bekreftelseTilstand: BekreftelseTilstand): BekreftelseProsesseringsResultat =
    (::opprettInitielBekreftelse andThen
            ::opprettManglendeBekreftelser andThen
            ::oppdaterBekreftelser andThen
            { (bekreftelser, hendelser) ->
                BekreftelseProsesseringsResultat(
                    oppdatertTilstand = bekreftelseTilstand.copy(bekreftelser = bekreftelser.toList()),
                    hendelser = hendelser,
                     gjeldendeTilstand = bekreftelseTilstand
                )
            })(bekreftelseTilstand.bekreftelser)

fun BekreftelseContext.opprettInitielBekreftelse(bekreftelser: List<Bekreftelse>): PawNonEmptyList<Bekreftelse> {
    val foerste = bekreftelser.firstOrNull { !it.dummy }
    return if (foerste != null) {
        pawNonEmptyListOf(foerste, bekreftelser.filter { it != foerste})
    } else {
        prometheusMeterRegistry.tellBekreftelseHandling("initiell_bekreftelse_opprettet")
        val fra = tidligsteBekreftelsePeriodeStart()
        val til = sluttTidForBekreftelsePeriode(
            startTid = fra,
            interval = konfigurasjon.interval
        )
        Span.current().addEvent(
            intern,
            Attributes.of(
                actionKey, bekreftelseOpprettetAction,
                initielBekreftelseKey, true,
                fraOgMedDagKey, fra.tilFraTilAttributeKeyValue(),
                tilDagKey, til.tilFraTilAttributeKeyValue()
            )
        )
        pawNonEmptyListOf(
            Bekreftelse(
                bekreftelseId = UUID.randomUUID(),
                tilstandsLogg = BekreftelseTilstandsLogg(
                    siste = IkkeKlarForUtfylling(wallClock.value),
                    tidligere = emptyList()
                ),
                gjelderFra = fra,
                gjelderTil = til,
            )
        )
    }
}

fun BekreftelseContext.opprettManglendeBekreftelser(bekreftelser: PawNonEmptyList<Bekreftelse>): PawNonEmptyList<Bekreftelse> {
    val siste = bekreftelser.maxBy { it.gjelderTil }
    val ventende = bekreftelser.toList().filter { it.sisteTilstand() is VenterPaaSvar }
    return if (siste.gjelderTil.isBefore(wallClock.value) && ventende.size < konfigurasjon.maksAntallVentendeBekreftelser) {
        prometheusMeterRegistry.tellBekreftelseHandling("bekreftelse_opprettet")
        val fra = siste.gjelderTil
        val til = sluttTidForBekreftelsePeriode(siste.gjelderTil, konfigurasjon.interval)
        Span.current().addEvent(
            intern,
            Attributes.of(
                actionKey, bekreftelseOpprettetAction,
                initielBekreftelseKey, false,
                fraOgMedDagKey, fra.tilFraTilAttributeKeyValue(),
                tilDagKey, til.tilFraTilAttributeKeyValue()
            )
        )
        val neste = Bekreftelse(
            bekreftelseId = UUID.randomUUID(),
            gjelderFra = fra,
            gjelderTil = til,
            tilstandsLogg = BekreftelseTilstandsLogg(
                siste = IkkeKlarForUtfylling(wallClock.value),
                tidligere = emptyList()
            )
        )
        bekreftelser + neste
    } else {
        bekreftelser
    }
}

fun BekreftelseContext.oppdaterBekreftelser(bekreftelser: PawNonEmptyList<Bekreftelse>): Pair<PawNonEmptyList<Bekreftelse>, List<BekreftelseHendelse>> =
    bekreftelser.map { bekreftelse ->
        when {
            bekreftelse.erKlarForUtfylling(wallClock.value, konfigurasjon.tilgjengeligOffset) -> {
                prometheusMeterRegistry.tellBekreftelseHandling("klar_for_utfylling")
                bekreftelse.setStatus(KlarForUtfylling(wallClock.value)) to BekreftelseTilgjengelig(
                    hendelseId = UUID.randomUUID(),
                    periodeId = periodeInfo.periodeId,
                    arbeidssoekerId = periodeInfo.arbeidsoekerId,
                    hendelseTidspunkt = wallClock.value,
                    bekreftelseId = bekreftelse.bekreftelseId,
                    gjelderFra = bekreftelse.gjelderFra,
                    gjelderTil = bekreftelse.gjelderTil
                )
            }

            bekreftelse.harFristUtloept(wallClock.value) -> {
                prometheusMeterRegistry.tellBekreftelseHandling("venter_svar")
                bekreftelse.setStatus(VenterSvar(wallClock.value)) to LeveringsfristUtloept(
                    hendelseId = UUID.randomUUID(),
                    periodeId = periodeInfo.periodeId,
                    arbeidssoekerId = periodeInfo.arbeidsoekerId,
                    hendelseTidspunkt = wallClock.value,
                    bekreftelseId = bekreftelse.bekreftelseId,
                    leveringsfrist = bekreftelse.gjelderTil
                )
            }

            bekreftelse.erSisteVarselOmGjenstaaendeGraceTid(
                wallClock.value,
                konfigurasjon.varselFoerGraceperiodeUtloept
            ) -> {
                prometheusMeterRegistry.tellBekreftelseHandling("varslet_om_graceperiode")
                bekreftelse.setStatus(GracePeriodeVarselet(wallClock.value)) to RegisterGracePeriodeGjenstaaendeTid(
                    hendelseId = UUID.randomUUID(),
                    periodeId = periodeInfo.periodeId,
                    arbeidssoekerId = periodeInfo.arbeidsoekerId,
                    hendelseTidspunkt = wallClock.value,
                    bekreftelseId = bekreftelse.bekreftelseId,
                    gjenstaandeTid = bekreftelse.gjenstaendeGraceperiode(
                        wallClock.value,
                        konfigurasjon.graceperiode
                    )
                )
            }

            bekreftelse.harGraceperiodeUtloept(wallClock.value, konfigurasjon.graceperiode) -> {
                prometheusMeterRegistry.tellBekreftelseHandling("graceperiode_utloept")
                bekreftelse.setStatus(GracePeriodeUtloept(wallClock.value)) to RegisterGracePeriodeUtloept(
                    hendelseId = UUID.randomUUID(),
                    periodeId = periodeInfo.periodeId,
                    arbeidssoekerId = periodeInfo.arbeidsoekerId,
                    hendelseTidspunkt = wallClock.value,
                    bekreftelseId = bekreftelse.bekreftelseId,
                    kilde = currentRuntimeEnvironment.appNameOrDefaultForLocal()
                )
            }

            else -> bekreftelse to null
        }
    }.let { bekreftelserOgHendelser ->
        val oppdaterteBekreftelser = bekreftelserOgHendelser.map { it.first }
        val hendelser = bekreftelserOgHendelser.toList().mapNotNull { it.second }
        oppdaterteBekreftelser to hendelser
    }

fun Bekreftelse.setStatus(nyStatus: BekreftelseTilstandStatus): Bekreftelse {
    val siste = sisteTilstand()
    Span.current().addEvent(
        intern,
        Attributes.of(
            actionKey, bekreftelseSattStatusAction,
            nyBekreftelseStatusKey, nyStatus::class.simpleName?.snakeCase() ?: "ukjent",
            gjeldeneBekreftelseStatusKey, siste::class.simpleName?.snakeCase() ?: "ukjent"
        )
    )
    return this + nyStatus
}