package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseMelding
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.*
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelsetjeneste.startdatohaandtering.StatiskMapOddetallPartallMap
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import no.nav.paw.test.assertEvent
import no.nav.paw.test.seconds
import java.time.Instant
import java.util.*

class BekreftelseStreamTest : FreeSpec({

    val identitetsnummer = "12345678901"
    val startTime = Instant.parse("2024-01-01T08:00:00Z")

    "For melding mottatt uten en tilhørende tilstand skal tilstand være uendret og hendelselogg skal være tom" {
        with(ApplicationTestContext(
            initialWallClockTime = startTime,
            oddetallPartallMap = StatiskMapOddetallPartallMap(emptySequence())
        )) {
            val bekreftelseMelding = bekreftelseMelding(
                periodeId = UUID.randomUUID(),
                bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER,
                gjelderFra = Instant.now(),
                gjelderTil = Instant.now(),
                harJobbetIDennePerioden = true,
                vilFortsetteSomArbeidssoeker = true
            )

            bekreftelseTopic.pipeInput(1234L, bekreftelseMelding)
            val stateStore =
                testDriver.getKeyValueStore<UUID, BekreftelseTilstand>(applicationConfig.kafkaTopology.internStateStoreName)
            stateStore.all().asSequence().count() shouldBe 0

            bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
        }
        println("hei" + Thread.currentThread().name)
    }

    "Mottatt melding med tilhørende tilstand GracePeriodeUtloept skal tilstand være uendret og hendelselogg skal være tom" {
        with(ApplicationTestContext(
            initialWallClockTime = startTime,
            oddetallPartallMap = StatiskMapOddetallPartallMap(emptySequence()))
        ) {
            with(kafkaKeyContext()) {
                val (_, _, interval, graceperiode, tilgjengeligOffset, varselFoerGraceperiodeUtloept) = bekreftelseKonfigurasjon
                (varselFoerGraceperiodeUtloept.multipliedBy(2) + 5.seconds) shouldBeGreaterThan graceperiode
                val (id, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(interval - tilgjengeligOffset + 5.seconds)
                val bekreftelse = bekreftelseHendelseloggTopicOut.assertEvent<BekreftelseTilgjengelig>()
                testDriver.advanceWallClockTime(tilgjengeligOffset + 5.seconds)
                bekreftelseHendelseloggTopicOut.assertEvent<LeveringsfristUtloept>()
                testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 5.seconds)
                bekreftelseHendelseloggTopicOut.assertEvent<RegisterGracePeriodeGjenstaaendeTid>()
                testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 50.seconds)
                bekreftelseHendelseloggTopicOut.assertEvent<RegisterGracePeriodeUtloept>()

                bekreftelseHendelseloggTopicOut.readRecordsToList()
                val bekreftelseMelding = bekreftelseMelding(
                    id = bekreftelse.bekreftelseId,
                    periodeId = periode.id,
                    bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                    gjelderFra = bekreftelse.gjelderFra,
                    gjelderTil = bekreftelse.gjelderTil,
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = true
                )

                bekreftelseTopic.pipeInput(key, bekreftelseMelding)

                val stateStore =
                    testDriver.getKeyValueStore<UUID, BekreftelseTilstand>(applicationConfig.kafkaTopology.internStateStoreName)
                val internTilstand = stateStore[periode.id]

                internTilstand should {
                    it.periode shouldBe PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = id,
                        recordKey = key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    )
                    it.bekreftelser.size shouldBe 2
                    it.bekreftelser.minBy { bekreftelse -> bekreftelse.gjelderFra } should { bekreftelse ->
                        bekreftelse.bekreftelseId shouldBe bekreftelseMelding.id
                        bekreftelse.gjelderFra shouldBe bekreftelseMelding.svar.gjelderFra
                        bekreftelse.gjelderTil shouldBe bekreftelseMelding.svar.gjelderTil
                        bekreftelse.tilstandsLogg
                            .asList()
                            .sortedBy(BekreftelseTilstandStatus::timestamp)
                            .map { it::class }
                            .shouldContainExactly(
                                IkkeKlarForUtfylling::class,
                                KlarForUtfylling::class,
                                VenterSvar::class,
                                GracePeriodeVarselet::class,
                                GracePeriodeUtloept::class
                            )
                        bekreftelse.sisteTilstand().shouldBeInstanceOf<GracePeriodeUtloept>()
                    }
                }
                bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
            }
        }
    }

    "Mottatt melding med tilhørende tilstand av typen VenterSvar skal oppdatere tilstand til Levert og sende BekreftelseMeldingMottatt hendelse".config(enabled = false) {
        with(ApplicationTestContext(
            initialWallClockTime = startTime,
            oddetallPartallMap = StatiskMapOddetallPartallMap(emptySequence()))
        ) {
            with(kafkaKeyContext()) {
                val (_, _, interval, _, tilgjengeligOffset, _) = bekreftelseKonfigurasjon
                val (id, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(
                    interval.minus(tilgjengeligOffset).plusSeconds(5)
                )

                val bekreftelseId = bekreftelseHendelseloggTopicOut.assertEvent { event: BekreftelseTilgjengelig ->
                    event.bekreftelseId
                }
                val bekreftelseMelding = bekreftelseMelding(
                    id = bekreftelseId,
                    periodeId = periode.id,
                    bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                    gjelderFra = startTime,
                    gjelderTil = sluttTidForBekreftelsePeriode(periode.startet.tidspunkt, interval),
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = true
                )
                bekreftelseTopic.pipeInput(key, bekreftelseMelding)

                val stateStore =
                    testDriver.getKeyValueStore<UUID, BekreftelseTilstand>(applicationConfig.kafkaTopology.internStateStoreName)
                val internTilstand = stateStore[periode.id]

                internTilstand should {
                    val periodeInfo = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = id,
                        recordKey = key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    )
                    it.periode shouldBe periodeInfo
                    it.bekreftelser.size shouldBe 1
                    it.bekreftelser.first().should { bekreftelse ->
                        bekreftelse.gjelderFra shouldBe bekreftelseMelding.svar.gjelderFra
                        bekreftelse.gjelderTil shouldBe bekreftelseMelding.svar.gjelderTil
                        bekreftelseId shouldBe bekreftelseMelding.id
                        bekreftelse.has<KlarForUtfylling>() shouldBe true
                        bekreftelse.has<Levert>() shouldBe true
                        bekreftelse.sisteTilstand().shouldBeInstanceOf<Levert>()
                    }
                }

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

    "Mottatt melding med tilhørende tilstand KlarForUtfylling skal oppdatere tilstand til Levert og sende BekreftelseMeldingMottatt og BaOmAaAvsluttePeriode hendelse om svaret er at bruker ikke vil fortsette som arbeidssøker".config(enabled = false) {
        with(ApplicationTestContext(
            initialWallClockTime = startTime,
            oddetallPartallMap = StatiskMapOddetallPartallMap(emptySequence()))
        ) {
            with(kafkaKeyContext()) {
                val (_, _, interval, _, tilgjengeligOffset, _) = bekreftelseKonfigurasjon
                val (id, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(
                    interval.minus(tilgjengeligOffset).plusSeconds(5)
                )

                val bekreftelseId = (bekreftelseHendelseloggTopicOut.readValue() as BekreftelseTilgjengelig).bekreftelseId
                val bekreftelseMelding = bekreftelseMelding(
                    id = bekreftelseId,
                    periodeId = periode.id,
                    bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                    gjelderFra = startTime,
                    gjelderTil = sluttTidForBekreftelsePeriode(periode.startet.tidspunkt, interval),
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = false
                )
                bekreftelseTopic.pipeInput(key, bekreftelseMelding)

                val stateStore =
                    testDriver.getKeyValueStore<UUID, BekreftelseTilstand>(applicationConfig.kafkaTopology.internStateStoreName)
                val internTilstand = stateStore[periode.id]

                internTilstand should {
                    it.periode shouldBe PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = id,
                        recordKey = key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    )
                    it.bekreftelser.size shouldBe 1
                    it.bekreftelser.first().should { bekreftelse ->
                        bekreftelse.gjelderFra shouldBe bekreftelseMelding.svar.gjelderFra
                        bekreftelse.gjelderTil shouldBe bekreftelseMelding.svar.gjelderTil
                        bekreftelse.bekreftelseId shouldBe bekreftelseMelding.id
                        bekreftelse.tilstandsLogg
                            .asList()
                            .sortedBy(BekreftelseTilstandStatus::timestamp)
                            .map { it::class }
                            .shouldContainExactly(
                                IkkeKlarForUtfylling::class,
                                KlarForUtfylling::class,
                                Levert::class
                            )
                        bekreftelse.sisteTilstand().shouldBeInstanceOf<Levert>()
                    }
                }

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
