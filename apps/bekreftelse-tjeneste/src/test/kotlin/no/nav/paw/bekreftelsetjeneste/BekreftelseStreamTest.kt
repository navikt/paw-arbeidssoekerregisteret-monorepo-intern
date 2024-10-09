package no.nav.paw.bekreftelsetjeneste

import io.kotest.assertions.withClue
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
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import org.apache.kafka.streams.TestOutputTopic
import java.time.Duration
import java.time.Instant
import java.util.*

inline fun <reified T: BekreftelseHendelse, A> TestOutputTopic<Long, BekreftelseHendelse>.assertEvent(function: (T) -> A): A =
    assertEvent<T>().let(function)

inline fun <reified T: BekreftelseHendelse> TestOutputTopic<Long, BekreftelseHendelse>.assertEvent(): T {
    withClue("Expected event of type ${T::class.simpleName}, no event was found") {
        isEmpty shouldBe false
    }
    withClue("Expected event of type ${T::class.simpleName}") {
        val event = readValue()
        return event.shouldBeInstanceOf<T>()
    }
}

val Int.seconds: Duration get() = Duration.ofSeconds(this.toLong())

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
        println("hei" + Thread.currentThread().name)
    }

    "Mottatt melding med tilhørende tilstand GracePeriodeUtloept skal tilstand være uendret og hendelselogg skal være tom" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (interval, graceperiode, tilgjengeligOffset, varselFoerGraceperiodeUtloept) = applicationConfig.bekreftelseIntervals
                (varselFoerGraceperiodeUtloept.multipliedBy(2) + 5.seconds) shouldBeGreaterThan graceperiode
                val (id, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(interval - tilgjengeligOffset + 5.seconds)
                val bekreftelseId = bekreftelseHendelseloggTopicOut.assertEvent { event: BekreftelseTilgjengelig ->
                    event.bekreftelseId
                }
                testDriver.advanceWallClockTime(tilgjengeligOffset + 5.seconds)
                bekreftelseHendelseloggTopicOut.assertEvent<LeveringsfristUtloept>()
                testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 5.seconds)
                bekreftelseHendelseloggTopicOut.assertEvent<RegisterGracePeriodeGjenstaaendeTid>()
                testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 50.seconds)
                bekreftelseHendelseloggTopicOut.assertEvent<RegisterGracePeriodeUtloept>()

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
                    it.bekreftelser.first() should { bekreftelse ->
                        bekreftelse.bekreftelseId shouldBe bekreftelseMelding.id
                        bekreftelse.gjelderFra shouldBe bekreftelseMelding.svar.gjelderFra
                        bekreftelse.gjelderTil shouldBe bekreftelseMelding.svar.gjelderTil
                        bekreftelse.tilstandsLogg
                            .asList()
                            .sortedBy(BekreftelseTilstand::timestamp)
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

    "Mottatt melding med tilhørende tilstand av typen VenterSvar skal oppdatere tilstand til Levert og sende BekreftelseMeldingMottatt hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (interval, _, tilgjengeligOffset, _) = applicationConfig.bekreftelseIntervals
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

                internTilstand should {
                    val periode = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = id,
                        recordKey = key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    )
                    it.periode shouldBe periode
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

    "Mottatt melding med tilhørende tilstand KlarForUtfylling skal oppdatere tilstand til Levert og sende BekreftelseMeldingMottatt og BaOmAaAvsluttePeriode hendelse om svaret er at bruker ikke vil fortsette som arbeidssøker" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (interval, _, tilgjengeligOffset, _) = applicationConfig.bekreftelseIntervals
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
                            .sortedBy(BekreftelseTilstand::timestamp)
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
