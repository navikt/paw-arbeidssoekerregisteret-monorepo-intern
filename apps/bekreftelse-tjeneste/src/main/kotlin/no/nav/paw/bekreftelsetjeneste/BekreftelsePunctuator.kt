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
            processBekreftelser(stateStore, key, value, timestamp, ctx)
        }
    }
}

private fun processBekreftelser(
    stateStore: StateStore,
    key: UUID,
    value: InternTilstand,
    timestamp: Instant,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
) {
    var updatedState: InternTilstand
    val existingBekreftelse = value.bekreftelser.firstOrNull()

    if (existingBekreftelse == null) {
        updatedState = handleNoExistingBekreftelse(value)
    } else {
        updatedState = handleExistingBekreftelser(value, timestamp, ctx)
    }

    updatedState = handleUpdateBekreftelser(updatedState, timestamp, ctx)

    stateStore.put(key, updatedState)
}

private fun handleNoExistingBekreftelse(value: InternTilstand): InternTilstand {
    val newBekreftelse = initBekreftelsePeriode(value.periode)
    val updatedInternTilstand = value.copy(bekreftelser = listOf(newBekreftelse))
    return updatedInternTilstand
}

private fun handleExistingBekreftelser(
    internTilstand: InternTilstand,
    timestamp: Instant,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
):InternTilstand {
    internTilstand.bekreftelser.toNonEmptyListOrNull()?.let { nonEmptyBekreftelser ->
        if (nonEmptyBekreftelser.skalLageNyBekreftelseTilgjengelig(timestamp)) {
            return createAndForwardNewBekreftelse(internTilstand, ctx)
        }
    }
    return internTilstand
}

private fun handleUpdateBekreftelser(
    internTilstand: InternTilstand,
    timestamp: Instant,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
): InternTilstand {
    internTilstand.bekreftelser.forEach { bekreftelse ->
        when {
            bekreftelse.tilstand == Tilstand.IkkeKlarForUtfylling && bekreftelse.erKlarForUtfylling(timestamp) ->
                return updateAndForwardBekreftelse(
                    internTilstand,
                    bekreftelse.copy(tilstand = Tilstand.KlarForUtfylling),
                    BekreftelseTilgjengelig(
                        hendelseId = UUID.randomUUID(),
                        periodeId = internTilstand.periode.periodeId,
                        arbeidssoekerId = internTilstand.periode.arbeidsoekerId,
                        bekreftelseId = bekreftelse.bekreftelseId,
                        gjelderFra = bekreftelse.gjelderFra,
                        gjelderTil = bekreftelse.gjelderTil
                    ),
                    ctx
                )

            bekreftelse.tilstand == Tilstand.KlarForUtfylling && bekreftelse.harFristUtloept(timestamp) ->
                return updateAndForwardBekreftelse(
                    internTilstand,
                    bekreftelse.copy(tilstand = Tilstand.VenterSvar),
                    LeveringsfristUtloept(
                        hendelseId = UUID.randomUUID(),
                        periodeId = internTilstand.periode.periodeId,
                        arbeidssoekerId = internTilstand.periode.arbeidsoekerId,
                        bekreftelseId = bekreftelse.bekreftelseId
                    ),
                    ctx
                )

            bekreftelse.tilstand == Tilstand.VenterSvar && bekreftelse.erSisteVarselOmGjenstaaendeGraceTid(timestamp) ->
                return updateAndForwardBekreftelse(
                    internTilstand,
                    bekreftelse.copy(sisteVarselOmGjenstaaendeGraceTid = timestamp),
                    RegisterGracePeriodeGjendstaaendeTid(
                        hendelseId = UUID.randomUUID(),
                        periodeId = internTilstand.periode.periodeId,
                        arbeidssoekerId = internTilstand.periode.arbeidsoekerId,
                        bekreftelseId = bekreftelse.bekreftelseId,
                        gjenstaandeTid = gjenstaendeGracePeriode(timestamp, bekreftelse.gjelderTil)
                    ),
                    ctx
                )

            bekreftelse.tilstand == Tilstand.VenterSvar && bekreftelse.harGracePeriodeUtloept(timestamp) ->
                return updateAndForwardBekreftelse(
                    internTilstand,
                    bekreftelse.copy(tilstand = Tilstand.GracePeriodeUtlopt),
                    RegisterGracePeriodeUtloept(
                        hendelseId = UUID.randomUUID(),
                        periodeId = internTilstand.periode.periodeId,
                        arbeidssoekerId = internTilstand.periode.arbeidsoekerId,
                        bekreftelseId = bekreftelse.bekreftelseId
                    ),
                    ctx
                )
        }
    }
    return internTilstand
}

private fun createAndForwardNewBekreftelse(
    internTilstand: InternTilstand,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
):InternTilstand {
    val newBekreftelse = initNyBekreftelsePeriode(internTilstand.bekreftelser)
    val updatedInternTilstand = internTilstand.copy(bekreftelser = internTilstand.bekreftelser + newBekreftelse)

    val record = Record(
        internTilstand.periode.recordKey,
        BekreftelseTilgjengelig(
            hendelseId = UUID.randomUUID(),
            periodeId = internTilstand.periode.periodeId,
            arbeidssoekerId = internTilstand.periode.arbeidsoekerId,
            bekreftelseId = newBekreftelse.bekreftelseId,
            gjelderFra = newBekreftelse.gjelderFra,
            gjelderTil = newBekreftelse.gjelderTil
        ),
        Instant.now().toEpochMilli()
    )
    ctx.forward(record)
    return updatedInternTilstand
}

private fun updateAndForwardBekreftelse(
    internTilstand: InternTilstand,
    updatedBekreftelse: Bekreftelse,
    hendelse: BekreftelseHendelse,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
):InternTilstand {
    val updatedInternTilstand =
        internTilstand.copy(bekreftelser = updatedBekreftelser(internTilstand.bekreftelser, updatedBekreftelse))

    val record = Record(
        internTilstand.periode.recordKey,
        hendelse,
        Instant.now().toEpochMilli()
    )
    ctx.forward(record)
    return updatedInternTilstand
}

private fun updatedBekreftelser(bekreftelser: List<Bekreftelse>, updatedBekreftelse: Bekreftelse) =
    bekreftelser.map { if (it.bekreftelseId == updatedBekreftelse.bekreftelseId) updatedBekreftelse else it }

private operator fun <K, V> KeyValue<K, V>.component1(): K = key
private operator fun <K, V> KeyValue<K, V>.component2(): V = value
