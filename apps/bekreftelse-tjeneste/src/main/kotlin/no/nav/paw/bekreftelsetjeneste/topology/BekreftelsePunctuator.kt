package no.nav.paw.bekreftelsetjeneste.topology

import arrow.core.toNonEmptyListOrNull
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
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

private val punctuatorLogger = LoggerFactory.getLogger("punctuator.bekreftelse")

fun bekreftelsePunctuator(
    bekreftelseTilstandStateStoreName: String,
    paaVegneAvTilstandStateStoreName: String,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    timestamp: Instant,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
) {
    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore = ctx.getStateStore(bekreftelseTilstandStateStoreName)
    val paaVegneAvTilstandStateStore: PaaVegneAvTilstandStateStore = ctx.getStateStore(paaVegneAvTilstandStateStoreName)

    bekreftelseTilstandStateStore
        .all()
        .use { states ->
            states
                .asSequence()
                .map { (_, tilstand) -> tilstand to paaVegneAvTilstandStateStore.get(tilstand.periode.periodeId) }
                .prosesserBekreftelseOgPaaVegneAvTilstand(bekreftelseKonfigurasjon, WallClock(timestamp))
                .forEach { (oppdatertTilstand, bekreftelseHendelser) ->
                    bekreftelseHendelser.forEach {
                        ctx.forward(Record(oppdatertTilstand.periode.recordKey, it, Instant.now().toEpochMilli()))
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
                wallClock.value
            )
        }

@WithSpan(
    value = "prosesser_bekreftelser",
    kind = SpanKind.INTERNAL
)
fun BekreftelseTilstand.prosesserBekreftelser(
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    currentTime: Instant,
): Pair<BekreftelseTilstand, List<BekreftelseHendelse>> {
    punctuatorLogger.trace("processBekreftelser: config='{}', state='{}', currentTime='{}'", bekreftelseKonfigurasjon, this, currentTime)
    val ingenBekreftelserITilstand = bekreftelser.firstOrNull() == null

    val (tilstand, hendelse) = if (ingenBekreftelserITilstand) {
        opprettInitiellBekreftelse(
            tidligsteStartTidspunktForBekreftelse = bekreftelseKonfigurasjon.migreringstidspunkt,
            interval = bekreftelseKonfigurasjon.interval
        ) to null
    } else {
        sjekkOgLagNyBekreftelse(currentTime, bekreftelseKonfigurasjon)
    }

    val (oppdatertTilstand, additionalHendelser) = tilstand.hentOppdatertBekreftelseTilstandOgGenererteHendelser(currentTime, bekreftelseKonfigurasjon)

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
    timestamp: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
): Pair<BekreftelseTilstand, BekreftelseHendelse?> {
    val nonEmptyBekreftelser = bekreftelser.toNonEmptyListOrNull() ?: return this to null

    return if (nonEmptyBekreftelser.skalOppretteNyBekreftelse(
            timestamp,
            bekreftelseKonfigurasjon.interval,
            bekreftelseKonfigurasjon.tilgjengeligOffset
        )
    ) {
        val newBekreftelse = nonEmptyBekreftelser.opprettNesteTilgjengeligeBekreftelse(
            tilgjengeliggjort = timestamp,
            interval = bekreftelseKonfigurasjon.interval,
        )
        copy(bekreftelser = nonEmptyBekreftelser + newBekreftelse) to opprettNyBekreftelseTilgjengeligHendelse(newBekreftelse)
    } else {
        this to null
    }
}

private fun BekreftelseTilstand.hentOppdatertBekreftelseTilstandOgGenererteHendelser(
    timestamp: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
): Pair<BekreftelseTilstand, List<BekreftelseHendelse>> =
    copy(bekreftelser = hentOppdaterteBekreftelser(timestamp, bekreftelseKonfigurasjon)) to hentGenererteHendelser(timestamp, bekreftelseKonfigurasjon)

private fun BekreftelseTilstand.hentOppdaterteBekreftelser(
    timestamp: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon
): List<Bekreftelse> =
    bekreftelser.map { bekreftelse ->
        prosesserBekreftelseTilstand(bekreftelse, timestamp, bekreftelseKonfigurasjon).last().first
    }

private fun BekreftelseTilstand.hentGenererteHendelser(
    timestamp: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon
): List<BekreftelseHendelse> =
    bekreftelser.flatMap { bekreftelse ->
        prosesserBekreftelseTilstand(bekreftelse, timestamp, bekreftelseKonfigurasjon).mapNotNull { it.second }
    }

private fun BekreftelseTilstand.prosesserBekreftelseTilstand(
    bekreftelse: Bekreftelse,
    timestamp: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon
): Sequence<Pair<Bekreftelse, BekreftelseHendelse?>> =
    generateSequence(bekreftelse to null as BekreftelseHendelse?) { (currentBekreftelse, _) ->
        hentProsessertBekreftelseTilstandOgHendelser(
            currentBekreftelse,
            timestamp,
            bekreftelseKonfigurasjon
        ).takeIf { it.second != null }
    }

private fun BekreftelseTilstand.hentProsessertBekreftelseTilstandOgHendelser(
    bekreftelse: Bekreftelse,
    timestamp: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
): Pair<Bekreftelse, BekreftelseHendelse?> {
    return when {
        bekreftelse.erKlarForUtfylling(timestamp, bekreftelseKonfigurasjon.tilgjengeligOffset) -> {
            val updatedBekreftelse = bekreftelse + KlarForUtfylling(timestamp)
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

        bekreftelse.harFristUtloept(timestamp, bekreftelseKonfigurasjon.tilgjengeligOffset) -> {
            val updatedBekreftelse = bekreftelse + VenterSvar(timestamp)
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

        bekreftelse.harGraceperiodeUtloept(timestamp, bekreftelseKonfigurasjon.graceperiode) -> {
            val updatedBekreftelse = bekreftelse + GracePeriodeUtloept(timestamp)
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
            timestamp,
            bekreftelseKonfigurasjon.varselFoerGraceperiodeUtloept
        ) -> {
            val updatedBekreftelse = bekreftelse + GracePeriodeVarselet(timestamp)
            val hendelse = RegisterGracePeriodeGjenstaaendeTid(
                hendelseId = UUID.randomUUID(),
                periodeId = periode.periodeId,
                arbeidssoekerId = periode.arbeidsoekerId,
                bekreftelseId = bekreftelse.bekreftelseId,
                gjenstaandeTid = bekreftelse.gjenstaendeGraceperiode(timestamp, bekreftelseKonfigurasjon.graceperiode),
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
