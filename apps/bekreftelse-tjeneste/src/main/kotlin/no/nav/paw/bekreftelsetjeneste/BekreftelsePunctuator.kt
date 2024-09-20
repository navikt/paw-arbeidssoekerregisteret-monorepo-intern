package no.nav.paw.bekreftelsetjeneste

import arrow.core.toNonEmptyListOrNull
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjendstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.Tilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.gjenstaendeGracePeriode
import no.nav.paw.bekreftelsetjeneste.tilstand.initBekreftelsePeriode
import no.nav.paw.bekreftelsetjeneste.tilstand.initNyBekreftelsePeriode
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import java.time.Instant
import java.util.*

fun bekreftelsePunctuator(
    stateStoreName: String,
    timestamp: Instant,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
) {
    val stateStore: StateStore = ctx.getStateStore(stateStoreName)

    stateStore.all().use { states ->
        states.forEach { (key, value) ->
            val (updatedState, bekreftelseHendelse) = processBekreftelser(value, timestamp)

            bekreftelseHendelse.forEach {
                ctx.forward(Record(value.periode.recordKey, it, Instant.now().toEpochMilli()))
            }
            stateStore.put(key, updatedState)
        }
    }
}

private fun processBekreftelser(
    currentState: InternTilstand,
    timestamp: Instant,
): Pair<InternTilstand, List<BekreftelseHendelse>> {
    val existingBekreftelse = currentState.bekreftelser.firstOrNull()

    val (tilstand, hendelse) = if (existingBekreftelse == null) {
        currentState.handleNoExistingBekreftelse() to null
    } else {
        currentState.handleExistingBekreftelser(timestamp)
    }

    val (updatedTilstand, additionalHendelser) = tilstand.handleUpdateBekreftelser(timestamp)

    return updatedTilstand to (listOfNotNull(hendelse) + additionalHendelser)
}

private fun InternTilstand.handleNoExistingBekreftelse(): InternTilstand =
    copy(bekreftelser = listOf(initBekreftelsePeriode(periode)))


private fun InternTilstand.handleExistingBekreftelser(
    timestamp: Instant
): Pair<InternTilstand, BekreftelseHendelse?> {
    val nonEmptyBekreftelser = bekreftelser.toNonEmptyListOrNull() ?: return this to null

    return if (nonEmptyBekreftelser.skalLageNyBekreftelseTilgjengelig(timestamp)) {
        val newBekreftelse = bekreftelser.initNyBekreftelsePeriode()
        copy(bekreftelser = nonEmptyBekreftelser + newBekreftelse) to createNewBekreftelseTilgjengelig(newBekreftelse)
    } else {
        this to null
    }
}

private fun InternTilstand.handleUpdateBekreftelser(
    timestamp: Instant,
): Pair<InternTilstand, List<BekreftelseHendelse>> =
    bekreftelser.map { bekreftelse ->
        when {
            bekreftelse.tilstand == Tilstand.IkkeKlarForUtfylling && bekreftelse.erKlarForUtfylling(timestamp) -> {
                val updatedBekreftelse = bekreftelse.copy(tilstand = Tilstand.KlarForUtfylling)
                val hendelse = BekreftelseTilgjengelig(
                    hendelseId = UUID.randomUUID(),
                    periodeId = periode.periodeId,
                    arbeidssoekerId = periode.arbeidsoekerId,
                    bekreftelseId = bekreftelse.bekreftelseId,
                    gjelderFra = bekreftelse.gjelderFra,
                    gjelderTil = bekreftelse.gjelderTil
                )
                updatedBekreftelse to hendelse
            }

            bekreftelse.tilstand == Tilstand.KlarForUtfylling && bekreftelse.harFristUtloept(timestamp) -> {
                val updatedBekreftelse = bekreftelse.copy(tilstand = Tilstand.VenterSvar)
                val hendelse = LeveringsfristUtloept(
                    hendelseId = UUID.randomUUID(),
                    periodeId = periode.periodeId,
                    arbeidssoekerId = periode.arbeidsoekerId,
                    bekreftelseId = bekreftelse.bekreftelseId
                )
                updatedBekreftelse to hendelse
            }

            bekreftelse.tilstand == Tilstand.VenterSvar && bekreftelse.erSisteVarselOmGjenstaaendeGraceTid(timestamp) -> {
                val updatedBekreftelse = bekreftelse.copy(sisteVarselOmGjenstaaendeGraceTid = timestamp)
                val hendelse = RegisterGracePeriodeGjendstaaendeTid(
                    hendelseId = UUID.randomUUID(),
                    periodeId = periode.periodeId,
                    arbeidssoekerId = periode.arbeidsoekerId,
                    bekreftelseId = bekreftelse.bekreftelseId,
                    gjenstaandeTid = gjenstaendeGracePeriode(timestamp, bekreftelse.gjelderTil)
                )
                updatedBekreftelse to hendelse
            }

            bekreftelse.tilstand == Tilstand.VenterSvar && bekreftelse.harGracePeriodeUtloept(timestamp) -> {
                val updatedBekreftelse = bekreftelse.copy(tilstand = Tilstand.GracePeriodeUtlopt)
                val hendelse = RegisterGracePeriodeUtloept(
                    hendelseId = UUID.randomUUID(),
                    periodeId = periode.periodeId,
                    arbeidssoekerId = periode.arbeidsoekerId,
                    bekreftelseId = bekreftelse.bekreftelseId
                )
                updatedBekreftelse to hendelse
            }

            else -> {
                bekreftelse to null
            }
        }
    }.let {
        val updatedBekreftelser = it.map { it.first }
        val hendelser = it.mapNotNull { it.second }
        copy(bekreftelser = updatedBekreftelser) to hendelser
    }

private fun InternTilstand.createNewBekreftelseTilgjengelig(newBekreftelse: Bekreftelse) =
    BekreftelseTilgjengelig(
        hendelseId = UUID.randomUUID(),
        periodeId = periode.periodeId,
        arbeidssoekerId = periode.arbeidsoekerId,
        bekreftelseId = newBekreftelse.bekreftelseId,
        gjelderFra = newBekreftelse.gjelderFra,
        gjelderTil = newBekreftelse.gjelderTil
    )

private operator fun <K, V> KeyValue<K, V>.component1(): K = key
private operator fun <K, V> KeyValue<K, V>.component2(): V = value
