package no.nav.paw.bekreftelsetjeneste.topology

import arrow.core.toNonEmptyListOrNull
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
    interntilstandStateStoreName: String,
    paaVegneAvTilstandStateStoreName: String,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    timestamp: Instant,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
) {
    val internTilstandStateStore: InternTilstandStateStore = ctx.getStateStore(interntilstandStateStoreName)
    val paaVegneAvTilstandStateStore: PaaVegneAvTilstandStateStore = ctx.getStateStore(paaVegneAvTilstandStateStoreName)

    internTilstandStateStore
        .all()
        .use { states ->
            states
                .asSequence()
                .map { (_, tilstand) -> tilstand to paaVegneAvTilstandStateStore.get(tilstand.periode.periodeId) }
                .prosessererBekreftelser(bekreftelseKonfigurasjon, WallClock(timestamp))
                .forEach { (updatedState, bekreftelseHendelser) ->
                    bekreftelseHendelser.forEach {
                        ctx.forward(Record(updatedState.periode.recordKey, it, Instant.now().toEpochMilli()))
                    }
                    internTilstandStateStore.put(updatedState.periode.periodeId, updatedState)
                }
        }
}

fun Sequence<Pair<BekreftelseTilstand, PaaVegneAvTilstand?>>.prosessererBekreftelser(
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    wallClock: WallClock
): Sequence<Pair<BekreftelseTilstand, List<BekreftelseHendelse>>> =
    filter { (internTilstand, paaVegneAvTilstand) ->
        (paaVegneAvTilstand == null)
            .also { result ->
                punctuatorLogger.trace(
                    "Periode {}, registeret har ansvar: {}",
                    internTilstand.periode.periodeId,
                    result
                )
            }
    }
        .map { (tilstand, _) ->
            processBekreftelser(
                bekreftelseKonfigurasjon,
                tilstand,
                wallClock.value
            )
        }

fun processBekreftelser(
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    currentState: BekreftelseTilstand,
    currentTime: Instant,
): Pair<BekreftelseTilstand, List<BekreftelseHendelse>> {
    punctuatorLogger.trace("processBekreftelser: config='{}', state='{}', currentTime='{}'", bekreftelseKonfigurasjon, currentState, currentTime)
    val existingBekreftelse = currentState.bekreftelser.firstOrNull()

    val (tilstand, hendelse) = if (existingBekreftelse == null) {
        currentState.createInitialBekreftelse(
            tidligsteStartTidspunktForBekreftelse = bekreftelseKonfigurasjon.migreringstidspunkt,
            interval = bekreftelseKonfigurasjon.interval
        ) to null
    } else {
        currentState.checkAndCreateNewBekreftelse(currentTime, bekreftelseKonfigurasjon)
    }

    val (updatedTilstand, additionalHendelse) = tilstand.handleUpdateBekreftelser(currentTime, bekreftelseKonfigurasjon)

    return updatedTilstand to listOfNotNull(hendelse, additionalHendelse)
}

private fun BekreftelseTilstand.createInitialBekreftelse(
    tidligsteStartTidspunktForBekreftelse: Instant,
    interval: Duration
): BekreftelseTilstand =
    copy(bekreftelser = listOf(opprettFoersteBekreftelse(
        tidligsteStartTidspunktForBekreftelse = tidligsteStartTidspunktForBekreftelse,
        periode = periode,
        interval = interval
    )))

private fun BekreftelseTilstand.checkAndCreateNewBekreftelse(
    timestamp: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
): Pair<BekreftelseTilstand, BekreftelseHendelse?> {
    val nonEmptyBekreftelser = bekreftelser.toNonEmptyListOrNull() ?: return this to null

    return if (nonEmptyBekreftelser.shouldCreateNewBekreftelse(
            timestamp,
            bekreftelseKonfigurasjon.interval,
            bekreftelseKonfigurasjon.tilgjengeligOffset
        )
    ) {
        val newBekreftelse = nonEmptyBekreftelser.opprettNesteTilgjengeligeBekreftelse(
            tilgjengeliggjort = timestamp,
            interval = bekreftelseKonfigurasjon.interval,
        )
        copy(bekreftelser = nonEmptyBekreftelser + newBekreftelse) to createNewBekreftelseTilgjengelig(newBekreftelse)
    } else {
        this to null
    }
}

private fun BekreftelseTilstand.handleUpdateBekreftelser(
    timestamp: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
): Pair<BekreftelseTilstand, BekreftelseHendelse?> {
    val updatedBekreftelser = bekreftelser.map { bekreftelse ->
        generateSequence(bekreftelse to null as BekreftelseHendelse?) { (currentBekreftelse, _) ->
            getProcessedBekreftelseTilstandAndHendelse(
                currentBekreftelse,
                timestamp,
                bekreftelseKonfigurasjon
            ).takeIf { it.second != null }
        }.last().first
    }

    val hendelse: BekreftelseHendelse? = bekreftelser.flatMap { bekreftelse ->
        generateSequence(bekreftelse to null as BekreftelseHendelse?) { (currentBekreftelse, _) ->
            getProcessedBekreftelseTilstandAndHendelse(
                currentBekreftelse,
                timestamp,
                bekreftelseKonfigurasjon
            ).takeIf { it.second != null }
        }.mapNotNull { it.second }
    }.lastOrNull()

    return copy(bekreftelser = updatedBekreftelser) to hendelse
}

private fun BekreftelseTilstand.getProcessedBekreftelseTilstandAndHendelse(
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

private fun BekreftelseTilstand.createNewBekreftelseTilgjengelig(newBekreftelse: Bekreftelse) =
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
