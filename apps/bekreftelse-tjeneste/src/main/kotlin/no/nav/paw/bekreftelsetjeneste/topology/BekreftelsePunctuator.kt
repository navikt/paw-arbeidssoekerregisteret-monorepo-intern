package no.nav.paw.bekreftelsetjeneste.topology

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.paavegneav.PaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import no.nav.paw.collections.toPawNonEmptyListOrNull
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

private val punctuatorLogger = LoggerFactory.getLogger("bekreftelse.tjeneste.punctuator")

@WithSpan(
    value = "bekreftelse_punctuator",
    kind = SpanKind.INTERNAL
)
fun bekreftelsePunctuator(
    bekreftelseTilstandStateStoreName: String,
    paaVegneAvTilstandStateStoreName: String,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    timestamp: Instant,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
) {
    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore = ctx.getStateStore(bekreftelseTilstandStateStoreName)
    val paaVegneAvTilstandStateStore: PaaVegneAvTilstandStateStore = ctx.getStateStore(paaVegneAvTilstandStateStoreName)
    Span.current().setAttribute(AttributeKey.longKey("partition"), ctx.taskId().partition())
    bekreftelseTilstandStateStore
        .all()
        .use { states ->
            states
                .asSequence()
                .map { (_, tilstand) -> tilstand to paaVegneAvTilstandStateStore.get(tilstand.periode.periodeId) }
                .prosesserBekreftelseOgPaaVegneAvTilstand(bekreftelseKonfigurasjon, WallClock(timestamp))
                .forEach { (oppdatertTilstand, bekreftelseHendelser) ->
                    bekreftelseHendelser.forEach {
                        ctx.forward(Record(oppdatertTilstand.periode.recordKey, it, ctx.currentSystemTimeMs()))
                    }
                    bekreftelseTilstandStateStore.put(oppdatertTilstand.periode.periodeId, oppdatertTilstand)
                }
        }
}

fun Sequence<Pair<BekreftelseTilstand, PaaVegneAvTilstand?>>.prosesserBekreftelseOgPaaVegneAvTilstand(
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    wallClock: WallClock
): Sequence<Pair<BekreftelseTilstand, List<BekreftelseHendelse>>> =
    filter { (bekreftelseTilstand, paaVegneAvTilstand) ->
        (paaVegneAvTilstand == null)
            .also { result ->
                punctuatorLogger.trace(
                    "Periode {}, registeret har ansvar: {}",
                    bekreftelseTilstand.periode.periodeId,
                    result
                )
            }
    }
        .map { (bekreftelseTilstand, _) ->
            bekreftelseTilstand.prosesserBekreftelser(
                bekreftelseKonfigurasjon,
                wallClock
            )
        }

@WithSpan(
    value = "prosesser_bekreftelser",
    kind = SpanKind.INTERNAL
)
fun BekreftelseTilstand.prosesserBekreftelser(
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    wallClock: WallClock,
): Pair<BekreftelseTilstand, List<BekreftelseHendelse>> {
    punctuatorLogger.trace("processBekreftelser: config='{}', state='{}', currentTime='{}'", bekreftelseKonfigurasjon, this, wallClock.value)
    val ingenBekreftelserITilstand = bekreftelser.firstOrNull() == null

    val (tilstand, hendelse) = if (ingenBekreftelserITilstand) {
        opprettInitiellBekreftelse(
            tidligsteStartTidspunktForBekreftelse = bekreftelseKonfigurasjon.migreringstidspunkt,
            interval = bekreftelseKonfigurasjon.interval
        ) to null
    } else {
        sjekkOgLagNyBekreftelse(wallClock, bekreftelseKonfigurasjon)
    }

    val (oppdatertTilstand, additionalHendelser) = tilstand.hentOppdatertBekreftelseTilstandOgGenererteHendelser(wallClock, bekreftelseKonfigurasjon)

    return oppdatertTilstand to listOfNotNull(hendelse) + additionalHendelser
}

private fun BekreftelseTilstand.opprettInitiellBekreftelse(
    tidligsteStartTidspunktForBekreftelse: Instant,
    interval: Duration
): BekreftelseTilstand =
    copy(bekreftelser = listOf(opprettFoersteBekreftelse(
        tidligsteStartTidspunktForBekreftelse = tidligsteStartTidspunktForBekreftelse,
        periode = periode,
        interval = interval
    )))

private fun BekreftelseTilstand.sjekkOgLagNyBekreftelse(
    wallclock: WallClock,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
): Pair<BekreftelseTilstand, BekreftelseHendelse?> {
    val nonEmptyBekreftelser = bekreftelser.toPawNonEmptyListOrNull() ?: return this to null

    return if (nonEmptyBekreftelser.skalOppretteNyBekreftelse(
            wallclock,
            bekreftelseKonfigurasjon.interval,
            bekreftelseKonfigurasjon.tilgjengeligOffset
        )
    ) {
        val newBekreftelse = nonEmptyBekreftelser.opprettNesteTilgjengeligeBekreftelse(
            wallClock = wallclock,
            interval = bekreftelseKonfigurasjon.interval,
        )
        copy(bekreftelser = (nonEmptyBekreftelser + newBekreftelse).toList()) to opprettNyBekreftelseTilgjengeligHendelse(newBekreftelse)
    } else {
        this to null
    }
}

private fun BekreftelseTilstand.hentOppdatertBekreftelseTilstandOgGenererteHendelser(
    wallClock: WallClock,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
): Pair<BekreftelseTilstand, List<BekreftelseHendelse>> =
    copy(bekreftelser = hentOppdaterteBekreftelser(wallClock, bekreftelseKonfigurasjon)) to hentGenererteHendelser(wallClock, bekreftelseKonfigurasjon)

private fun BekreftelseTilstand.hentOppdaterteBekreftelser(
    wallClock: WallClock,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon
): List<Bekreftelse> =
    bekreftelser.map { bekreftelse ->
        prosesserBekreftelseTilstand(bekreftelse, wallClock, bekreftelseKonfigurasjon).last().first
    }

private fun BekreftelseTilstand.hentGenererteHendelser(
    wallClock: WallClock,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon
): List<BekreftelseHendelse> =
    bekreftelser.flatMap { bekreftelse ->
        prosesserBekreftelseTilstand(bekreftelse, wallClock, bekreftelseKonfigurasjon).mapNotNull { it.second }
    }

private fun BekreftelseTilstand.prosesserBekreftelseTilstand(
    bekreftelse: Bekreftelse,
    wallClock: WallClock,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon
): Sequence<Pair<Bekreftelse, BekreftelseHendelse?>> =
    generateSequence(bekreftelse to null as BekreftelseHendelse?) { (currentBekreftelse, _) ->
        hentProsessertBekreftelseTilstandOgHendelser(
            currentBekreftelse,
            wallClock,
            bekreftelseKonfigurasjon
        ).takeIf { it.second != null }
    }

private fun BekreftelseTilstand.hentProsessertBekreftelseTilstandOgHendelser(
    bekreftelse: Bekreftelse,
    wallClock: WallClock,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
): Pair<Bekreftelse, BekreftelseHendelse?> {
    return when {
        bekreftelse.erKlarForUtfylling(wallClock.value, bekreftelseKonfigurasjon.tilgjengeligOffset) -> {
            val updatedBekreftelse = bekreftelse + KlarForUtfylling(wallClock.value)
            val hendelse = BekreftelseTilgjengelig(
                hendelseId = UUID.randomUUID(),
                periodeId = periode.periodeId,
                arbeidssoekerId = periode.arbeidsoekerId,
                bekreftelseId = bekreftelse.bekreftelseId,
                gjelderFra = bekreftelse.gjelderFra,
                gjelderTil = bekreftelse.gjelderTil,
                hendelseTidspunkt = Instant.now()
            )
            updatedBekreftelse to hendelse
        }

        bekreftelse.harFristUtloept(wallClock.value, bekreftelseKonfigurasjon.tilgjengeligOffset) -> {
            val updatedBekreftelse = bekreftelse + VenterSvar(wallClock.value)
            val hendelse = LeveringsfristUtloept(
                hendelseId = UUID.randomUUID(),
                periodeId = periode.periodeId,
                arbeidssoekerId = periode.arbeidsoekerId,
                bekreftelseId = bekreftelse.bekreftelseId,
                hendelseTidspunkt = Instant.now(),
                leveringsfrist = bekreftelse.gjelderTil
            )
            updatedBekreftelse to hendelse
        }

        bekreftelse.harGraceperiodeUtloept(wallClock.value, bekreftelseKonfigurasjon.graceperiode) -> {
            val updatedBekreftelse = bekreftelse + GracePeriodeUtloept(wallClock.value)
            val hendelse = RegisterGracePeriodeUtloept(
                hendelseId = UUID.randomUUID(),
                periodeId = periode.periodeId,
                arbeidssoekerId = periode.arbeidsoekerId,
                bekreftelseId = bekreftelse.bekreftelseId,
                hendelseTidspunkt = Instant.now()
            )
            updatedBekreftelse to hendelse
        }

        bekreftelse.erSisteVarselOmGjenstaaendeGraceTid(
            wallClock.value,
            bekreftelseKonfigurasjon.varselFoerGraceperiodeUtloept
        ) -> {
            val updatedBekreftelse = bekreftelse + GracePeriodeVarselet(wallClock.value)
            val hendelse = RegisterGracePeriodeGjenstaaendeTid(
                hendelseId = UUID.randomUUID(),
                periodeId = periode.periodeId,
                arbeidssoekerId = periode.arbeidsoekerId,
                bekreftelseId = bekreftelse.bekreftelseId,
                gjenstaandeTid = bekreftelse.gjenstaendeGraceperiode(wallClock.value, bekreftelseKonfigurasjon.graceperiode),
                hendelseTidspunkt = Instant.now()
            )
            updatedBekreftelse to hendelse
        }

        else -> {
            bekreftelse to null
        }
    }
}

private fun BekreftelseTilstand.opprettNyBekreftelseTilgjengeligHendelse(newBekreftelse: Bekreftelse) =
    BekreftelseTilgjengelig(
        hendelseId = UUID.randomUUID(),
        periodeId = periode.periodeId,
        arbeidssoekerId = periode.arbeidsoekerId,
        bekreftelseId = newBekreftelse.bekreftelseId,
        gjelderFra = newBekreftelse.gjelderFra,
        gjelderTil = newBekreftelse.gjelderTil,
        hendelseTidspunkt = Instant.now()
    )

private operator fun <K, V> KeyValue<K, V>.component1(): K = key
private operator fun <K, V> KeyValue<K, V>.component2(): V = value
