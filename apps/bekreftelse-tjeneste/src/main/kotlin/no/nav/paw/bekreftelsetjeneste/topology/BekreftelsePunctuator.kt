package no.nav.paw.bekreftelsetjeneste.topology

import arrow.core.toNonEmptyListOrNull
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseIntervals
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
    ansvarStateStoreName: String,
    bekreftelseIntervals: BekreftelseIntervals,
    timestamp: Instant,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
) {
    val internTilstandStateStore: InternTilstandStateStore = ctx.getStateStore(interntilstandStateStoreName)
    val ansvarStateStore: AnsvarStateStore = ctx.getStateStore(ansvarStateStoreName)

    internTilstandStateStore
        .all()
        .use { states ->
            states
                .asSequence()
                .filter { (_, value) -> (ansvarStateStore.get(value.periode.periodeId) == null)
                    .also { result ->
                        punctuatorLogger.trace("Periode {}, registeret har ansvar: {}", value.periode.periodeId, result)
                    }}
                .forEach { (key, value) ->
                    val (updatedState, bekreftelseHendelser) = processBekreftelser(
                        bekreftelseIntervals,
                        value,
                        timestamp
                    )
                    punctuatorLogger.trace("Wallclocktime: {}", timestamp)
                    punctuatorLogger.trace(
                        "Eksiterende bekreftelser: {} {}",
                        value.bekreftelser,
                        if (value.bekreftelser.isEmpty()) ", periode startet: ${value.periode.startet}" else ""
                    )
                    punctuatorLogger.trace("Oppdaterte bekreftelser: {}", updatedState.bekreftelser)
                    bekreftelseHendelser.forEach {
                        ctx.forward(Record(value.periode.recordKey, it, Instant.now().toEpochMilli()))
                    }
                    internTilstandStateStore.put(key, updatedState)
                }
        }
}

fun processBekreftelser(
    bekreftelseIntervals: BekreftelseIntervals,
    currentState: InternTilstand,
    currentTime: Instant,
): Pair<InternTilstand, List<BekreftelseHendelse>> {
    val existingBekreftelse = currentState.bekreftelser.firstOrNull()

    val (tilstand, hendelse) = if (existingBekreftelse == null) {
        currentState.createInitialBekreftelse(bekreftelseIntervals.interval, currentTime) to null
    } else {
        currentState.checkAndCreateNewBekreftelse(currentTime, bekreftelseIntervals)
    }

    val (updatedTilstand, additionalHendelse) = tilstand.handleUpdateBekreftelser(currentTime, bekreftelseIntervals)

    return updatedTilstand to listOfNotNull(hendelse, additionalHendelse)
}

private fun InternTilstand.createInitialBekreftelse(interval: Duration, currentTime: Instant): InternTilstand =
    copy(bekreftelser = listOf(opprettFoersteBekreftelse(periode, interval, currentTime)))

private fun InternTilstand.checkAndCreateNewBekreftelse(
    timestamp: Instant,
    bekreftelseIntervals: BekreftelseIntervals,
): Pair<InternTilstand, BekreftelseHendelse?> {
    val nonEmptyBekreftelser = bekreftelser.toNonEmptyListOrNull() ?: return this to null

    return if (nonEmptyBekreftelser.shouldCreateNewBekreftelse(
            timestamp,
            bekreftelseIntervals.interval,
            bekreftelseIntervals.tilgjengeligOffset
        )
    ) {
        val newBekreftelse = nonEmptyBekreftelser.opprettNesteTilgjengeligeBekreftelse(
            tilgjengeliggjort = timestamp,
            interval = bekreftelseIntervals.interval,
        )
        copy(bekreftelser = nonEmptyBekreftelser + newBekreftelse) to createNewBekreftelseTilgjengelig(newBekreftelse)
    } else {
        this to null
    }
}

private fun InternTilstand.handleUpdateBekreftelser(
    timestamp: Instant,
    bekreftelseIntervals: BekreftelseIntervals,
): Pair<InternTilstand, BekreftelseHendelse?> {
    val updatedBekreftelser = bekreftelser.map { bekreftelse ->
        generateSequence(bekreftelse to null as BekreftelseHendelse?) { (currentBekreftelse, _) ->
            getProcessedBekreftelseTilstandAndHendelse(
                currentBekreftelse,
                timestamp,
                bekreftelseIntervals
            ).takeIf { it.second != null }
        }.last().first
    }

    val hendelse: BekreftelseHendelse? = bekreftelser.flatMap { bekreftelse ->
        generateSequence(bekreftelse to null as BekreftelseHendelse?) { (currentBekreftelse, _) ->
            getProcessedBekreftelseTilstandAndHendelse(
                currentBekreftelse,
                timestamp,
                bekreftelseIntervals
            ).takeIf { it.second != null }
        }.mapNotNull { it.second }
    }.lastOrNull()

    return copy(bekreftelser = updatedBekreftelser) to hendelse
}

private fun InternTilstand.getProcessedBekreftelseTilstandAndHendelse(
    bekreftelse: Bekreftelse,
    timestamp: Instant,
    bekreftelseIntervals: BekreftelseIntervals,
): Pair<Bekreftelse, BekreftelseHendelse?> {
    return when {
        bekreftelse.erKlarForUtfylling(timestamp, bekreftelseIntervals.tilgjengeligOffset) -> {
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

        bekreftelse.harFristUtloept(timestamp, bekreftelseIntervals.tilgjengeligOffset) -> {
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

        bekreftelse.harGraceperiodeUtloept(timestamp, bekreftelseIntervals.graceperiode) -> {
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
            bekreftelseIntervals.varselFoerGraceperiodeUtloept
        ) -> {
            val updatedBekreftelse = bekreftelse + GracePeriodeVarselet(timestamp)
            val hendelse = RegisterGracePeriodeGjenstaaendeTid(
                hendelseId = UUID.randomUUID(),
                periodeId = periode.periodeId,
                arbeidssoekerId = periode.arbeidsoekerId,
                bekreftelseId = bekreftelse.bekreftelseId,
                gjenstaandeTid = bekreftelse.gjenstaendeGraceperiode(timestamp, bekreftelseIntervals.graceperiode),
                hendelseTidspunkt = Instant.now()
            )
            updatedBekreftelse to hendelse
        }

        else -> {
            bekreftelse to null
        }
    }
}

private fun InternTilstand.createNewBekreftelseTilgjengelig(newBekreftelse: Bekreftelse) =
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
