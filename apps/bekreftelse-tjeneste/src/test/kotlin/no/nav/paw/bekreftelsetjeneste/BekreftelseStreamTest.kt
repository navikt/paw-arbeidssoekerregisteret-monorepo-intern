package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseMelding
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.PeriodeInfo
import no.nav.paw.bekreftelsetjeneste.tilstand.Tilstand
import java.time.Instant
import java.util.*


class BekreftelseStreamTest : FreeSpec({

    val identitetsnummer = "12345678901"
    val startTime = Instant.ofEpochMilli(1704185347000)

    "For melding mottatt uten en tilhørende tilstand skal tilstand være uendret og hendelselogg skal være tom" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            val bekreftelseMelding = bekreftelseMelding(
                periodeId = UUID.randomUUID(),
                namespace = "tullball",
                gjelderFra = Instant.now(),
                gjelderTil = Instant.now(),
                harJobbetIDennePerioden = true,
                vilFortsetteSomArbeidssoeker = true
            )

            bekreftelseTopic.pipeInput(1234L, bekreftelseMelding)
            val stateStore =
                testDriver.getKeyValueStore<UUID, InternTilstand>(applicationConfig.kafkaTopology.internStateStoreName)
            stateStore.all().asSequence().count() shouldBe 0

            bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
        }
    }

    "Mottatt melding med tilhørende tilstand GracePeriodeUtloept skal tilstand være uendret og hendelselogg skal være tom" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (interval, _, tilgjengeligOffset, varselFoerGraceperiodeUtloept) = applicationConfig.bekreftelseIntervals
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(
                    interval.minus(tilgjengeligOffset).plusSeconds(5)
                )
                val bekreftelseId = (bekreftelseHendelseloggTopicOut.readValue() as BekreftelseTilgjengelig).bekreftelseId
                testDriver.advanceWallClockTime(tilgjengeligOffset.plusSeconds(5))
                testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept.plusSeconds(5))
                testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept.plusSeconds(5))

                bekreftelseHendelseloggTopicOut.readRecordsToList()
                val bekreftelseMelding = bekreftelseMelding(
                    id = bekreftelseId,
                    periodeId = periode.id,
                    namespace = "paw",
                    gjelderFra = startTime,
                    gjelderTil = startTime.plus(interval),
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = true
                )

                bekreftelseTopic.pipeInput(key, bekreftelseMelding)

                val stateStore =
                    testDriver.getKeyValueStore<UUID, InternTilstand>(applicationConfig.kafkaTopology.internStateStoreName)
                val internTilstand = stateStore[periode.id]

                internTilstand shouldBe InternTilstand(
                    periode = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = id,
                        recordKey = key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    ),
                    bekreftelser = listOf(
                        no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse(
                            tilstand = Tilstand.GracePeriodeUtloept,
                            tilgjengeliggjort = startTime.plus(
                                interval.minus(tilgjengeligOffset)
                                    .plusSeconds(5)
                            ),
                            fristUtloept = startTime.plus(interval).plusSeconds(10),
                            sisteVarselOmGjenstaaendeGraceTid = startTime.plus(interval).plus(varselFoerGraceperiodeUtloept).plusSeconds(15),
                            bekreftelseId = bekreftelseMelding.id,
                            gjelderFra = bekreftelseMelding.svar.gjelderFra,
                            gjelderTil = bekreftelseMelding.svar.gjelderTil
                        )
                    )
                )

                bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
            }
        }
    }

    "Mottatt melding med tilhørende tilstand av typen VenterSvar skal oppdatere tilstand til Levert og sende BekreftelseMeldingMottatt hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (interval, _, tilgjengeligOffset, _) = applicationConfig.bekreftelseIntervals
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(
                    interval.minus(tilgjengeligOffset).plusSeconds(5)
                )

                val bekreftelseId = (bekreftelseHendelseloggTopicOut.readValue() as BekreftelseTilgjengelig).bekreftelseId
                val bekreftelseMelding = bekreftelseMelding(
                    id = bekreftelseId,
                    periodeId = periode.id,
                    namespace = "paw",
                    gjelderFra = startTime,
                    gjelderTil = startTime.plus(interval),
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = true
                )
                bekreftelseTopic.pipeInput(key, bekreftelseMelding)

                val stateStore =
                    testDriver.getKeyValueStore<UUID, InternTilstand>(applicationConfig.kafkaTopology.internStateStoreName)
                val internTilstand = stateStore[periode.id]

                internTilstand shouldBe InternTilstand(
                    periode = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = id,
                        recordKey = key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    ),
                    bekreftelser = listOf(
                        no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse(
                            tilstand = Tilstand.Levert,
                            tilgjengeliggjort = startTime.plus(
                                interval.minus(tilgjengeligOffset)
                                    .plusSeconds(5)
                            ),
                            fristUtloept = null,
                            sisteVarselOmGjenstaaendeGraceTid = null,
                            bekreftelseId = bekreftelseMelding.id,
                            gjelderFra = bekreftelseMelding.svar.gjelderFra,
                            gjelderTil = bekreftelseMelding.svar.gjelderTil
                        )
                    )
                )

                bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                val hendelse = bekreftelseHendelseloggTopicOut.readKeyValue()
                hendelse.key shouldBe key
                hendelse.value shouldBe BekreftelseMeldingMottatt(
                    hendelseId = hendelse.value.hendelseId,
                    periodeId = periode.id,
                    arbeidssoekerId = id,
                    bekreftelseId = bekreftelseMelding.id,
                    hendelseTidspunkt = hendelse.value.hendelseTidspunkt
                )
            }
        }
    }

    "Mottatt melding med tilhørende tilstand KlarForUtfylling skal oppdatere tilstand til Levert og sende BekreftelseMeldingMottatt og BaOmAaAvsluttePeriode hendelse om svaret er at bruker ikke vil fortsette som arbeidssøker" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (interval, _, tilgjengeligOffset, _) = applicationConfig.bekreftelseIntervals
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(
                    interval.minus(tilgjengeligOffset).plusSeconds(5)
                )

                val bekreftelseId = (bekreftelseHendelseloggTopicOut.readValue() as BekreftelseTilgjengelig).bekreftelseId
                val bekreftelseMelding = bekreftelseMelding(
                    id = bekreftelseId,
                    periodeId = periode.id,
                    namespace = "paw",
                    gjelderFra = startTime,
                    gjelderTil = startTime.plus(interval),
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = false
                )
                bekreftelseTopic.pipeInput(key, bekreftelseMelding)

                val stateStore =
                    testDriver.getKeyValueStore<UUID, InternTilstand>(applicationConfig.kafkaTopology.internStateStoreName)
                val internTilstand = stateStore[periode.id]

                internTilstand shouldBe InternTilstand(
                    periode = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = id,
                        recordKey = key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    ),
                    bekreftelser = listOf(
                        no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse(
                            tilstand = Tilstand.Levert,
                            tilgjengeliggjort = startTime.plus(
                                interval.minus(tilgjengeligOffset)
                                    .plusSeconds(5)
                            ),
                            fristUtloept = null,
                            sisteVarselOmGjenstaaendeGraceTid = null,
                            bekreftelseId = bekreftelseMelding.id,
                            gjelderFra = bekreftelseMelding.svar.gjelderFra,
                            gjelderTil = bekreftelseMelding.svar.gjelderTil,
                        )
                    )
                )

                bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                val hendelse = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                hendelse[0].key shouldBe key
                hendelse[0].value.shouldBeInstanceOf<BekreftelseMeldingMottatt>()
                hendelse[1].key shouldBe key
                hendelse[1].value.shouldBeInstanceOf<BaOmAaAvsluttePeriode>()
            }
        }
    }

})
