package no.nav.paw.bekreftelsetjeneste.topology

import arrow.core.toNonEmptyListOrNull
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.ansvar.Ansvar
import no.nav.paw.bekreftelsetjeneste.ansvar.WallClock
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
    ansvarStateStoreName: String,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
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
                .map { (_, tilstand) -> tilstand to ansvarStateStore.get(tilstand.periode.periodeId) }
                .prosessererBekreftelser(bekreftelseKonfigurasjon, WallClock(timestamp))
                .forEach { (updatedState, bekreftelseHendelser) ->
                    bekreftelseHendelser.forEach {
                        ctx.forward(Record(updatedState.periode.recordKey, it, Instant.now().toEpochMilli()))
                    }
                    internTilstandStateStore.put(updatedState.periode.periodeId, updatedState)
                }
        }
}

fun Sequence<Pair<InternTilstand, Ansvar?>>.prosessererBekreftelser(
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    wallClock: WallClock
): Sequence<Pair<InternTilstand, List<BekreftelseHendelse>>> =
    filter { (internTilstand, ansvar) ->
        (ansvar == null)
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
    currentState: InternTilstand,
    currentTime: Instant,
): Pair<InternTilstand, List<BekreftelseHendelse>> {
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

private fun InternTilstand.createInitialBekreftelse(
    tidligsteStartTidspunktForBekreftelse: Instant,
    interval: Duration
): InternTilstand =
    copy(bekreftelser = listOf(opprettFoersteBekreftelse(
        tidligsteStartTidspunktForBekreftelse = tidligsteStartTidspunktForBekreftelse,
        periode = periode,
        interval = interval
    )))

private fun InternTilstand.checkAndCreateNewBekreftelse(
    timestamp: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
): Pair<InternTilstand, BekreftelseHendelse?> {
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

private fun InternTilstand.handleUpdateBekreftelser(
    timestamp: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
): Pair<InternTilstand, BekreftelseHendelse?> {
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

private fun InternTilstand.getProcessedBekreftelseTilstandAndHendelse(
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
