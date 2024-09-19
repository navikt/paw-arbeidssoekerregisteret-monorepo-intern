package no.nav.paw.bekreftelsetjeneste

import arrow.core.nonEmptyListOf
import arrow.core.tail
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjendstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.Tilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.fristForNesteBekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.gjenstaendeGracePeriode
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import java.time.Instant
import java.util.*

fun bekreftelsePunctuator(stateStoreName: String, timestamp: Instant, ctx: ProcessorContext<Long, BekreftelseHendelse> ) {
    val stateStore: StateStore = ctx.getStateStore(stateStoreName)

    stateStore.all().use { states ->
        states.forEach { (key, value) ->
            val existingBekreftelse = value.bekreftelser.firstOrNull()
            if (existingBekreftelse == null){
                val newBekreftelse = Bekreftelse(
                    tilstand = Tilstand.IkkeKlarForUtfylling,
                    sisteVarselOmGjenstaaendeGraceTid = null,
                    bekreftelseId = UUID.randomUUID(),
                    gjelderFra = value.periode.startet,
                    gjelderTil = fristForNesteBekreftelse(value.periode.startet, BekreftelseConfig.bekreftelseInterval)
                )
                val updatedInternTilstand = value.copy(
                    bekreftelser = value.bekreftelser + newBekreftelse
                )
                stateStore.put(key, updatedInternTilstand)
            } else if (skalLageNyBekreftelseTilgjengelig(timestamp, nonEmptyListOf(existingBekreftelse, *value.bekreftelser.tail().toTypedArray()))) {
                val newBekreftelse = value.bekreftelser.last().copy(
                    tilstand = Tilstand.KlarForUtfylling,
                    sisteVarselOmGjenstaaendeGraceTid = null,
                    bekreftelseId = UUID.randomUUID(),
                    gjelderFra = value.bekreftelser.last().gjelderTil,
                    gjelderTil = fristForNesteBekreftelse(value.bekreftelser.last().gjelderTil, BekreftelseConfig.bekreftelseInterval)

                )
                val updatedInternTilstand = value.copy(
                    bekreftelser = value.bekreftelser + newBekreftelse
                )
                stateStore.put(key, updatedInternTilstand)

                val record = Record<Long, BekreftelseHendelse>(
                    value.periode.recordKey,
                    BekreftelseTilgjengelig(
                        hendelseId = UUID.randomUUID(),
                        periodeId = value.periode.periodeId,
                        arbeidssoekerId = value.periode.arbeidsoekerId,
                        bekreftelseId = newBekreftelse.bekreftelseId,
                        gjelderFra = newBekreftelse.gjelderFra,
                        gjelderTil = newBekreftelse.gjelderTil
                    ),
                    Instant.now().toEpochMilli()
                )

                ctx.forward(record)
            }
            value.bekreftelser.forEach { bekreftelse ->
                when {
                    bekreftelse.tilstand == Tilstand.IkkeKlarForUtfylling
                            && bekreftelse.erKlarForUtfylling(timestamp) -> {
                        val updatedBekreftelse = bekreftelse.copy(tilstand = Tilstand.KlarForUtfylling)
                        val updatedInternTilstand =
                            value.copy(bekreftelser = value.bekreftelser - bekreftelse + updatedBekreftelse)

                        stateStore.put(key, updatedInternTilstand)

                        val record = Record<Long, BekreftelseHendelse>(
                            value.periode.recordKey,
                            BekreftelseTilgjengelig(
                                hendelseId = UUID.randomUUID(),
                                periodeId = value.periode.periodeId,
                                arbeidssoekerId = value.periode.arbeidsoekerId,
                                bekreftelseId = bekreftelse.bekreftelseId,
                                gjelderFra = bekreftelse.gjelderFra,
                                gjelderTil = bekreftelse.gjelderTil
                            ),
                            Instant.now().toEpochMilli()
                        )
                        ctx.forward(record)
                    }

                    bekreftelse.tilstand == Tilstand.KlarForUtfylling
                            && bekreftelse.harFristUtloept(timestamp) -> {
                        val updatedBekreftelse = bekreftelse.copy(tilstand = Tilstand.VenterSvar)
                        val updatedInternTilstand =
                            value.copy(bekreftelser = value.bekreftelser - bekreftelse + updatedBekreftelse)

                        stateStore.put(key, updatedInternTilstand)

                        val record = Record<Long, BekreftelseHendelse>(
                            value.periode.recordKey,
                            LeveringsfristUtloept(
                                hendelseId = UUID.randomUUID(),
                                periodeId = value.periode.periodeId,
                                arbeidssoekerId = value.periode.arbeidsoekerId,
                                bekreftelseId = bekreftelse.bekreftelseId,
                            ),
                            Instant.now().toEpochMilli()
                        )
                        ctx.forward(record)
                    }

                    bekreftelse.tilstand == Tilstand.VenterSvar && bekreftelse.erSisteVarselOmGjenstaaendeGraceTid(timestamp) -> {
                        val updatedBekreftelse = bekreftelse.copy(sisteVarselOmGjenstaaendeGraceTid = timestamp)
                        val updatedInternTilstand =
                            value.copy(bekreftelser = value.bekreftelser - bekreftelse + updatedBekreftelse)


                        stateStore.put(key, updatedInternTilstand)

                        val record = Record<Long, BekreftelseHendelse>(
                            value.periode.recordKey,
                            RegisterGracePeriodeGjendstaaendeTid(
                                hendelseId = UUID.randomUUID(),
                                periodeId = value.periode.periodeId,
                                arbeidssoekerId = value.periode.arbeidsoekerId,
                                bekreftelseId = bekreftelse.bekreftelseId,
                                gjenstaandeTid = gjenstaendeGracePeriode(timestamp, bekreftelse.gjelderTil)
                            ),
                            Instant.now().toEpochMilli()
                        )
                        ctx.forward(record)
                    }

                    bekreftelse.tilstand == Tilstand.VenterSvar && bekreftelse.harGracePeriodeUtloept(timestamp) -> {

                        val updatedBekreftelse = bekreftelse.copy(tilstand = Tilstand.GracePeriodeUtlopt)
                        val updatedInternTilstand = value.copy(
                            bekreftelser = value.bekreftelser - bekreftelse + updatedBekreftelse
                        )

                        stateStore.put(key, updatedInternTilstand)

                        val record = Record<Long, BekreftelseHendelse>(
                            value.periode.recordKey,
                            RegisterGracePeriodeUtloept(
                                hendelseId = UUID.randomUUID(),
                                periodeId = value.periode.periodeId,
                                arbeidssoekerId = value.periode.arbeidsoekerId,
                                bekreftelseId = bekreftelse.bekreftelseId
                            ),
                            Instant.now().toEpochMilli()
                        )

                        ctx.forward(record)
                    }
                }
            }
        }
    }
}

private operator fun <K, V> KeyValue<K, V>.component1(): K = key
private operator fun <K, V> KeyValue<K, V>.component2(): V = value
