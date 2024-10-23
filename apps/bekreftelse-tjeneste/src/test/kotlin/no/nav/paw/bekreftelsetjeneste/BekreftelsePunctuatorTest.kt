package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import no.nav.paw.bekreftelsetjeneste.topology.InternTilstandStateStore
import java.time.Duration
import java.time.Instant

class BekreftelsePunctuatorTest : FreeSpec({

    val startTime = Instant.parse("2024-01-01T08:00:00Z")
    val identitetsnummer = "12345678901"

    "BekreftelsePunctuator sender riktig hendelser i rekkefølge" - {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (interval, graceperiode, tilgjengeligOffset, varselFoerGraceperiodeUtloept) = applicationConfig.bekreftelseIntervals
                val (id, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )
                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))

                "Når perioden opprettes skal det opprettes en intern tilstand med en bekreftelse" {
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)

                    internTilstandStateStore.get(periode.id) should { currentState ->
                        currentState.periode shouldBe PeriodeInfo(
                            periodeId = periode.id,
                            identitetsnummer = periode.identitetsnummer,
                            arbeidsoekerId = id,
                            recordKey = key,
                            startet = periode.startet.tidspunkt,
                            avsluttet = periode.avsluttet?.tidspunkt
                        )
                        currentState.bekreftelser.size shouldBe 1
                        currentState.bekreftelser.first() should { bekreftelse ->
                            bekreftelse.gjelderFra shouldBe periode.startet.tidspunkt
                            bekreftelse.gjelderTil shouldBe fristForNesteBekreftelse(
                                periode.startet.tidspunkt,
                                interval,
                                startTime
                            )
                            bekreftelse.tilstandsLogg.asList().size shouldBe 1
                            bekreftelse.has<IkkeKlarForUtfylling>() shouldBe true
                        }
                    }
                }

                "Etter 11 dager skal det ha blitt sendt en BekreftelseTilgjengelig hendelse" {
                    testDriver.advanceWallClockTime(interval.minus(tilgjengeligOffset))
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    internTilstandStateStore.all().use {
                        it.forEach {
                            logger.info("key: ${it.key}, value: ${it.value}")
                        }
                    }
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                    val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                    logger.info("hendelser: $hendelser")
                    hendelser.size shouldBe 1
                    val kv = hendelser.last()
                    kv.key shouldBe key
                    kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()
                }

                "Etter 14 dager skal det ha blitt sendt en LeveringsFristUtloept hendelse" {
                    testDriver.advanceWallClockTime(tilgjengeligOffset.plusSeconds(5))
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    internTilstandStateStore.all().use {
                        it.forEach {
                            logger.info("key: ${it.key}, value: ${it.value}")
                        }
                    }
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                    val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                    logger.info("hendelser: $hendelser")
                    hendelser.size shouldBe 1
                    val hendelseLast = hendelser.last()
                    hendelseLast.key shouldBe key
                    hendelseLast.value.shouldBeInstanceOf<LeveringsfristUtloept>()
                }
                "Etter 17,5 dager uten svar skal det ha blitt sendt en RegisterGracePeriodeGjenstaaendeTid hendelse" {
                    testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept.plusSeconds(5))
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    internTilstandStateStore.all().use {
                        it.forEach {
                            logger.info("key: ${it.key}, value: ${it.value}")
                        }
                    }
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                    val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                    logger.info("hendelser: $hendelser")
                    hendelser.size shouldBe 1
                    val kv = hendelser.last()
                    kv.key shouldBe key
                    kv.value.shouldBeInstanceOf<RegisterGracePeriodeGjenstaaendeTid>()
                }
                "Etter 21 dager uten svar skal det ha blitt sendt en RegisterGracePeriodeUtloept hendelse" {
                    testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept.plusSeconds(5))
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    internTilstandStateStore.all().use {
                        it.forEach {
                            logger.info("key: ${it.key}, value: ${it.value}")
                        }
                    }
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                    val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                    logger.info("hendelser: $hendelser")
                    hendelser.size shouldBe 1
                    val kv = hendelser.last()
                    kv.key shouldBe key
                    kv.value.shouldBeInstanceOf<RegisterGracePeriodeUtloept>()
                }
                "Etter 25 dager skal det ha blitt sendt en BekreftelseTilgjengelig hendelse" {
                    testDriver.advanceWallClockTime(
                        Duration.between(
                            startTime.plus(interval).plus(graceperiode),
                            startTime.plus(interval)
                                .plus(interval.minus(tilgjengeligOffset))
                        )
                    )
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    internTilstandStateStore.all().use {
                        it.forEach {
                            logger.info("key: ${it.key}, value: ${it.value}")
                        }
                    }
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                    val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                    logger.info("hendelser: $hendelser")
                    hendelser.size shouldBe 1
                    val kv = hendelser.last()
                    kv.key shouldBe key
                    kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()
                }
            }
        }
    }
    "BekreftelsePunctuator håndterer BekreftelseMeldingMottatt hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (interval, graceperiode, tilgjengeligOffset, _) = applicationConfig.bekreftelseIntervals
                val (_, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(
                    interval.minus(tilgjengeligOffset)
                        .plusSeconds(5)
                )
                val internTilstandStateStore: InternTilstandStateStore =
                    testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                val currentState = internTilstandStateStore.get(periode.id)
                bekreftelseTopic.pipeInput(
                    key, no.nav.paw.bekreftelse.melding.v1.Bekreftelse(
                        periode.id, Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET, currentState.bekreftelser.first().bekreftelseId, Svar(
                            Metadata(
                                Instant.now(), no.nav.paw.bekreftelse.melding.v1.vo.Bruker(
                                    BrukerType.SLUTTBRUKER, "12345678901"
                                ), "test", "test"
                            ),
                            periode.startet.tidspunkt,
                            fristForNesteBekreftelse(periode.startet.tidspunkt, interval, startTime),
                            true,
                            true
                        )
                    )
                )
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                testDriver.advanceWallClockTime(tilgjengeligOffset.plus(graceperiode))
                val hendelseLoggOutput = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                logger.info("hendelseOutput: $hendelseLoggOutput")
                internTilstandStateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggOutput.size shouldBe 2
                hendelseLoggOutput.filter { it.value is BekreftelseTilgjengelig }.size shouldBe 1
                hendelseLoggOutput.filter { it.value is BekreftelseMeldingMottatt }.size shouldBe 1
            }

        }
    }
    "BekreftelsePunctuator håndterer BaOmAaAvslutePeriode hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (interval, graceperiode, tilgjengeligOffset, _) = applicationConfig.bekreftelseIntervals
                val (_, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(
                    interval.minus(tilgjengeligOffset)
                        .plusSeconds(5)
                )
                val internTilstandStateStore: InternTilstandStateStore =
                    testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                val currentState = internTilstandStateStore.get(periode.id)
                bekreftelseTopic.pipeInput(
                    key, no.nav.paw.bekreftelse.melding.v1.Bekreftelse(
                        periode.id, Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET, currentState.bekreftelser.first().bekreftelseId, Svar(
                            Metadata(
                                Instant.now(), no.nav.paw.bekreftelse.melding.v1.vo.Bruker(
                                    BrukerType.SLUTTBRUKER, "12345678901"
                                ), "test", "test"
                            ),
                            periode.startet.tidspunkt,
                            fristForNesteBekreftelse(periode.startet.tidspunkt, interval, startTime),
                            true,
                            false
                        )
                    )
                )
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                testDriver.advanceWallClockTime(tilgjengeligOffset.plus(graceperiode))
                val hendelseLoggOutput = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                logger.info("hendelseOutput: $hendelseLoggOutput")
                internTilstandStateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggOutput.size shouldBe 3

                hendelseLoggOutput.filter { it.value is BekreftelseTilgjengelig }.size shouldBe 1
                hendelseLoggOutput.filter { it.value is BekreftelseMeldingMottatt }.size shouldBe 1
                hendelseLoggOutput.filter { it.value is BaOmAaAvsluttePeriode }.size shouldBe 1
            }
        }
    }
    "BekreftelsePunctuator setter riktig tilstand og sender riktig hendelse for: " - {
        "IkkeKlarForUtfylling" {
            with(ApplicationTestContext(initialWallClockTime = startTime)) {
                with(kafkaKeyContext()) {
                    val (interval, _, _, _) = applicationConfig.bekreftelseIntervals
                    val (id, key, periode) = periode(
                        identitetsnummer = identitetsnummer,
                        startetMetadata = metadata(tidspunkt = startTime)
                    )
                    periodeTopic.pipeInput(key, periode)
                    testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    val currentState = internTilstandStateStore.get(periode.id)
                    currentState should {
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
                            bekreftelse.gjelderFra shouldBe periode.startet.tidspunkt
                            bekreftelse.gjelderTil shouldBe fristForNesteBekreftelse(
                                periode.startet.tidspunkt,
                                interval,
                                startTime
                            )
                            bekreftelse.tilstandsLogg.asList().size shouldBe 1
                            bekreftelse.has<IkkeKlarForUtfylling>() shouldBe true
                        }
                    }
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
                }
            }
        }
        "KlarForUtfylling og BekreftelseTilgjengelig" {
            with(ApplicationTestContext(initialWallClockTime = startTime)) {
                with(kafkaKeyContext()) {
                    val (interval, _, tilgjengeligOffset, _) = applicationConfig.bekreftelseIntervals
                    val (id, key, periode) = periode(
                        identitetsnummer = identitetsnummer,
                        startetMetadata = metadata(tidspunkt = startTime)
                    )

                    periodeTopic.pipeInput(key, periode)
                    testDriver.advanceWallClockTime(
                        interval.minus(tilgjengeligOffset)
                            .plusSeconds(5)
                    )
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    val currentState = internTilstandStateStore.get(periode.id)
                    currentState should {
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
                            bekreftelse.gjelderFra shouldBe periode.startet.tidspunkt
                            bekreftelse.gjelderTil shouldBe fristForNesteBekreftelse(
                                periode.startet.tidspunkt,
                                interval,
                                startTime
                            )
                            bekreftelse.tilstandsLogg.asList().size shouldBe 2
                            bekreftelse.has<IkkeKlarForUtfylling>() shouldBe true
                            bekreftelse.has<KlarForUtfylling>() shouldBe true
                        }
                    }
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                    val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                    hendelser.size shouldBe 1
                    val kv = hendelser.last()
                    kv.key shouldBe key
                    kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()
                }
            }
        }
        "VenterSvar og LeveringsfristUtloept" {
            with(ApplicationTestContext(initialWallClockTime = startTime)) {
                with(kafkaKeyContext()) {
                    val (interval, _, tilgjengeligOffset, _) = applicationConfig.bekreftelseIntervals
                    val (id, key, periode) = periode(
                        identitetsnummer = identitetsnummer,
                        startetMetadata = metadata(tidspunkt = startTime)
                    )
                    periodeTopic.pipeInput(key, periode)
                    // Spoler frem til BekreftelseTilgjengelig
                    testDriver.advanceWallClockTime(
                        interval.minus(tilgjengeligOffset)
                            .plusSeconds(5)
                    )
                    // Spoler frem til LeveringsfristUtloept
                    testDriver.advanceWallClockTime(tilgjengeligOffset.plusSeconds(5))
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    internTilstandStateStore.all().use {
                        it.forEach {
                            logger.info("key: ${it.key}, value: ${it.value}")
                        }
                    }
                    val currentState = internTilstandStateStore.get(periode.id)
                    currentState should {
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
                            bekreftelse.gjelderFra shouldBe periode.startet.tidspunkt
                            bekreftelse.gjelderTil shouldBe fristForNesteBekreftelse(
                                periode.startet.tidspunkt,
                                interval,
                                startTime
                            )
                            bekreftelse.tilstandsLogg.asList().size shouldBe 3
                            bekreftelse.has<IkkeKlarForUtfylling>() shouldBe true
                            bekreftelse.has<KlarForUtfylling>() shouldBe true
                            bekreftelse.has<VenterSvar>() shouldBe true
                        }
                    }
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                    val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                    logger.info("hendelser: $hendelser")
                    hendelser.size shouldBe 2
                    val kv = hendelser.last()
                    kv.key shouldBe key
                    kv.value.shouldBeInstanceOf<LeveringsfristUtloept>()
                }
            }
        }
        "VenterSvar og RegisterGracePeriodeGjenstaaende" {
            with(ApplicationTestContext(initialWallClockTime = startTime)) {
                with(kafkaKeyContext()) {
                    val (interval, _, tilgjengeligOffset, varselFoerGraceperiodeUtloept) = applicationConfig.bekreftelseIntervals
                    val (id, key, periode) = periode(
                        identitetsnummer = identitetsnummer,
                        startetMetadata = metadata(tidspunkt = startTime)
                    )
                    periodeTopic.pipeInput(key, periode)
                    // Spoler frem til BekreftelseTilgjengelig
                    testDriver.advanceWallClockTime(
                        interval.minus(tilgjengeligOffset)
                            .plusSeconds(5)
                    )
                    // Spoler frem til LeveringsfristUtloept
                    testDriver.advanceWallClockTime(tilgjengeligOffset.plusSeconds(5))
                    // Spoler frem til RegisterGracePeriodeGjenstaaende
                    testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept.plusSeconds(5))
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    val currentState = internTilstandStateStore.get(periode.id)
                    currentState should {
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
                            bekreftelse.gjelderFra shouldBe periode.startet.tidspunkt
                            bekreftelse.gjelderTil shouldBe fristForNesteBekreftelse(
                                periode.startet.tidspunkt,
                                interval,
                                startTime
                            )
                            bekreftelse.tilstandsLogg.asList().size shouldBe 4
                            bekreftelse.has<IkkeKlarForUtfylling>() shouldBe true
                            bekreftelse.has<KlarForUtfylling>() shouldBe true
                            bekreftelse.has<VenterSvar>() shouldBe true
                            bekreftelse.has<GracePeriodeVarselet>() shouldBe true
                        }
                    }
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                    val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                    logger.info("hendelser: $hendelser")
                    hendelser.size shouldBe 3
                    val kv = hendelser.last()
                    kv.key shouldBe key
                    kv.value.shouldBeInstanceOf<RegisterGracePeriodeGjenstaaendeTid>()
                }
            }
        }
        "GracePeriodeUtloept og RegisterGracePeriodeUtloept" {
            with(ApplicationTestContext(initialWallClockTime = startTime)) {
                with(kafkaKeyContext()) {
                    val (interval, _, tilgjengeligOffset, varselFoerGraceperiodeUtloept) = applicationConfig.bekreftelseIntervals
                    val (id, key, periode) = periode(
                        identitetsnummer = identitetsnummer,
                        startetMetadata = metadata(tidspunkt = startTime)
                    )
                    periodeTopic.pipeInput(key, periode)
                    // Spoler frem til BekreftelseTilgjengelig
                    testDriver.advanceWallClockTime(
                        interval.minus(tilgjengeligOffset)
                            .plusSeconds(5)
                    )
                    // Spoler frem til LeveringsfristUtloept
                    testDriver.advanceWallClockTime(tilgjengeligOffset.plusSeconds(5))
                    // Spoler frem til RegisterGracePeriodeGjenstaaendeTid
                    testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept.plusSeconds(5))
                    // Spoler frem til RegisterGracePeriodeUtloept
                    testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept.plusSeconds(5))
                    val internTilstandStateStore: InternTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    val currentState = internTilstandStateStore.get(periode.id)
                    currentState should {
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
                            bekreftelse.gjelderFra shouldBe periode.startet.tidspunkt
                            bekreftelse.gjelderTil shouldBe fristForNesteBekreftelse(
                                periode.startet.tidspunkt,
                                interval,
                                startTime
                            )
                            bekreftelse.tilstandsLogg.asList().size shouldBe 5
                            bekreftelse.has<IkkeKlarForUtfylling>() shouldBe true
                            bekreftelse.has<KlarForUtfylling>() shouldBe true
                            bekreftelse.has<VenterSvar>() shouldBe true
                            bekreftelse.has<GracePeriodeVarselet>() shouldBe true
                            bekreftelse.has<GracePeriodeUtloept>() shouldBe true
                        }
                    }
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                    val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                    logger.info("hendelser: $hendelser")
                    hendelser.size shouldBe 4
                    val kv = hendelser.last()
                    kv.key shouldBe key
                    kv.value.shouldBeInstanceOf<RegisterGracePeriodeUtloept>()
                }
            }
        }
    }
})
