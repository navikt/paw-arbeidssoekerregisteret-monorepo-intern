package no.nav.paw.bekreftelsetjeneste

import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjendstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.Tilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.fristForNesteBekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.gjenstaendeGracePeriode
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import java.time.Instant
import java.util.*

fun scheduleUpdateTilstand(timestamp: Instant, ctx: ProcessorContext<Long, BekreftelseHendelse>, stateStoreNames: Array<out String>) {
    val stateStore: StateStore = ctx.getStateStore(stateStoreNames[0])

    stateStore.all().use { states ->
        states.forEach { (key, value) ->
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

                    bekreftelse.tilstand == Tilstand.VenterSvar && bekreftelse.skalPurres(timestamp) -> {
                        val updatedBekreftelse = bekreftelse.copy(sistePurring = timestamp)
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

                        // TODO: Mangler vi tilstand.utloept eller er det riktig at den skal fjernes her?
                        val updatedInternTilstand = value.copy(
                            bekreftelser = value.bekreftelser - bekreftelse
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

                    bekreftelse.skalLageNyBekreftelseTilgjengelig(timestamp, value.bekreftelser) -> {
                        val newBekreftelse = bekreftelse.copy(
                            tilstand = Tilstand.KlarForUtfylling,
                            sistePurring = null,
                            bekreftelseId = UUID.randomUUID(),
                            gjelderFra = bekreftelse.gjelderTil,
                            gjelderTil = fristForNesteBekreftelse(bekreftelse.gjelderTil, BekreftelseConfig.bekreftelseInterval)

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
                }
            }
        }
    }
}

private operator fun <K, V> KeyValue<K, V>.component1(): K = key
private operator fun <K, V> KeyValue<K, V>.component2(): V = value
