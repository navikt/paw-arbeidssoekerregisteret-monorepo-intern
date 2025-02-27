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
import no.nav.paw.bekreftelsetjeneste.topology.BekreftelseTilstandStateStore
import no.nav.paw.test.days
import no.nav.paw.test.seconds
import java.time.Duration
import java.time.Instant
import java.util.UUID

class BekreftelsePunctuatorTest : FreeSpec({

    val startTime = Instant.parse("2024-01-01T08:00:00Z")
    val identitetsnummer = "12345678901"

    "BekreftelsePunctuator sender riktig hendelser i rekkefølge" - {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (_,
                    interval,
                    graceperiode,
                    tilgjengeligOffset,
                    varselFoerGraceperiodeUtloept) = bekreftelseKonfigurasjon
                val (id, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )
                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(5.seconds)

                "Når perioden opprettes skal det opprettes en intern tilstand med en bekreftelse" {
                    bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)

                    bekreftelseTilstandStateStore.get(periode.id) should { currentState ->
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
                            bekreftelse.gjelderTil shouldBe sluttTidForBekreftelsePeriode(
                                periode.startet.tidspunkt,
                                interval
                            )
                            bekreftelse.tilstandsLogg.asList().size shouldBe 1
                            bekreftelse.has<IkkeKlarForUtfylling>() shouldBe true
                        }
                    }
                }

                "Etter 11 dager skal det ha blitt sendt en BekreftelseTilgjengelig hendelse" {
                    testDriver.advanceWallClockTime(interval - tilgjengeligOffset)
                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    bekreftelseTilstandStateStore.all().use {
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
                    testDriver.advanceWallClockTime(tilgjengeligOffset + 5.seconds)
                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    bekreftelseTilstandStateStore.all().use {
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
                    testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 5.seconds)
                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    bekreftelseTilstandStateStore.all().use {
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
                    testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 5.seconds)
                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    bekreftelseTilstandStateStore.all().use {
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
                            startTime + interval + graceperiode,
                            startTime + interval + (interval - tilgjengeligOffset)
                        )
                    )
                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    bekreftelseTilstandStateStore.all().use {
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
                val (_, interval, graceperiode, tilgjengeligOffset, _) = bekreftelseKonfigurasjon
                val (_, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime( interval - tilgjengeligOffset + 5.seconds)

                val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                    testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                val currentState = bekreftelseTilstandStateStore.get(periode.id)
                bekreftelseTopic.pipeInput(
                    key, no.nav.paw.bekreftelse.melding.v1.Bekreftelse(
                        periode.id,
                        Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                        currentState.bekreftelser.first().bekreftelseId,
                        Svar(
                            Metadata(
                                Instant.now(), no.nav.paw.bekreftelse.melding.v1.vo.Bruker(
                                    BrukerType.SLUTTBRUKER, "12345678901"
                                ), "test", "test"
                            ),
                            periode.startet.tidspunkt,
                            sluttTidForBekreftelsePeriode(periode.startet.tidspunkt, interval),
                            true,
                            true
                        )
                    )
                )

                testDriver.advanceWallClockTime(5.seconds)
                testDriver.advanceWallClockTime(tilgjengeligOffset + graceperiode)

                val hendelseLoggOutput = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                logger.info("hendelseOutput: $hendelseLoggOutput")
                bekreftelseTilstandStateStore.all().use {
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
                val (_, interval, graceperiode, tilgjengeligOffset, _) = bekreftelseKonfigurasjon
                val (_, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )

                periodeTopic.pipeInput(key, periode)
                testDriver.advanceWallClockTime(interval - tilgjengeligOffset + 5.seconds)
                val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                    testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                val currentState = bekreftelseTilstandStateStore.get(periode.id)
                bekreftelseTopic.pipeInput(
                    key, no.nav.paw.bekreftelse.melding.v1.Bekreftelse(
                        periode.id,
                        Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                        currentState.bekreftelser.first().bekreftelseId,
                        Svar(
                            Metadata(
                                Instant.now(), no.nav.paw.bekreftelse.melding.v1.vo.Bruker(
                                    BrukerType.SLUTTBRUKER, "12345678901"
                                ), "test", "test"
                            ),
                            periode.startet.tidspunkt,
                            sluttTidForBekreftelsePeriode(periode.startet.tidspunkt, interval),
                            true,
                            false
                        )
                    )
                )

                testDriver.advanceWallClockTime(5.seconds)
                testDriver.advanceWallClockTime(tilgjengeligOffset + graceperiode)

                val hendelseLoggOutput = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                logger.info("hendelseOutput: $hendelseLoggOutput")
                bekreftelseTilstandStateStore.all().use {
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
                    val (_, interval, _, _, _) = bekreftelseKonfigurasjon
                    val (id, key, periode) = periode(
                        identitetsnummer = identitetsnummer,
                        startetMetadata = metadata(tidspunkt = startTime)
                    )
                    periodeTopic.pipeInput(key, periode)
                    testDriver.advanceWallClockTime(5.seconds)

                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    val currentState = bekreftelseTilstandStateStore.get(periode.id)

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
                            bekreftelse.gjelderTil shouldBe sluttTidForBekreftelsePeriode(
                                periode.startet.tidspunkt,
                                interval
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
                    val (_, interval, _, tilgjengeligOffset, _) = bekreftelseKonfigurasjon
                    val (id, key, periode) = periode(
                        identitetsnummer = identitetsnummer,
                        startetMetadata = metadata(tidspunkt = startTime)
                    )
                    periodeTopic.pipeInput(key, periode)

                    testDriver.advanceWallClockTime(interval - tilgjengeligOffset + 5.seconds)

                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    val currentState = bekreftelseTilstandStateStore.get(periode.id)

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
                            bekreftelse.gjelderTil shouldBe sluttTidForBekreftelsePeriode(
                                periode.startet.tidspunkt,
                                interval
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
                    val (_, interval, _, tilgjengeligOffset, _) = bekreftelseKonfigurasjon
                    val (id, key, periode) = periode(
                        identitetsnummer = identitetsnummer,
                        startetMetadata = metadata(tidspunkt = startTime)
                    )
                    periodeTopic.pipeInput(key, periode)

                    // Spoler frem til BekreftelseTilgjengelig
                    testDriver.advanceWallClockTime(interval - tilgjengeligOffset + 5.seconds)
                    // Spoler frem til LeveringsfristUtloept
                    testDriver.advanceWallClockTime(tilgjengeligOffset + 5.seconds)

                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    bekreftelseTilstandStateStore.all().use {
                        it.forEach {
                            logger.info("key: ${it.key}, value: ${it.value}")
                        }
                    }
                    val currentState = bekreftelseTilstandStateStore.get(periode.id)
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
                            bekreftelse.gjelderTil shouldBe sluttTidForBekreftelsePeriode(
                                periode.startet.tidspunkt,
                                interval
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
                    val (_, interval, _, tilgjengeligOffset, varselFoerGraceperiodeUtloept) = bekreftelseKonfigurasjon
                    val (id, key, periode) = periode(
                        identitetsnummer = identitetsnummer,
                        startetMetadata = metadata(tidspunkt = startTime)
                    )
                    periodeTopic.pipeInput(key, periode)

                    // Spoler frem til BekreftelseTilgjengelig
                    testDriver.advanceWallClockTime(interval - tilgjengeligOffset + 5.seconds)
                    // Spoler frem til LeveringsfristUtloept
                    testDriver.advanceWallClockTime(tilgjengeligOffset + 5.seconds)
                    // Spoler frem til RegisterGracePeriodeGjenstaaende
                    testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 5.seconds)

                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    val currentState = bekreftelseTilstandStateStore.get(periode.id)
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
                            bekreftelse.gjelderTil shouldBe sluttTidForBekreftelsePeriode(
                                periode.startet.tidspunkt,
                                interval
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
                    val (_, interval, _, tilgjengeligOffset, varselFoerGraceperiodeUtloept) = bekreftelseKonfigurasjon
                    val (id, key, periode) = periode(
                        identitetsnummer = identitetsnummer,
                        startetMetadata = metadata(tidspunkt = startTime)
                    )
                    periodeTopic.pipeInput(key, periode)

                    // Spoler frem til BekreftelseTilgjengelig
                    testDriver.advanceWallClockTime(interval - tilgjengeligOffset + 5.seconds)
                    // Spoler frem til LeveringsfristUtloept
                    testDriver.advanceWallClockTime(tilgjengeligOffset + 5.seconds)
                    // Spoler frem til RegisterGracePeriodeGjenstaaendeTid
                    testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 5.seconds)
                    // Spoler frem til RegisterGracePeriodeUtloept
                    testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 5.seconds)

                    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
                        testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                    val currentState = bekreftelseTilstandStateStore.get(periode.id)
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
                            bekreftelse.gjelderTil shouldBe sluttTidForBekreftelsePeriode(
                                periode.startet.tidspunkt,
                                interval
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

    "BekreftelsePunctuator sender RegisterGraceperiodeUtloept hendelse om første av to bekreftelser sin graceperiode utløper" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (_, interval, _, tilgjengeligOffset, varselFoerGraceperiodeUtloept) = bekreftelseKonfigurasjon
                val (_, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )
                periodeTopic.pipeInput(key, periode)

                testDriver.advanceWallClockTime(interval - tilgjengeligOffset + 5.seconds)

                val stateStore: BekreftelseTilstandStateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                val bekreftelseTilstand = stateStore.get(periode.id)
                val bekreftelse1 = bekreftelseTilstand.bekreftelser.first()
                val sisteTilstandsLogg = bekreftelse1.tilstandsLogg.siste.timestamp
                val bekreftelseTilgjengeligSisteTilstandStatus = KlarForUtfylling(sisteTilstandsLogg + 1.days)
                val bekreftelse2 = bekreftelse1.copy(
                    bekreftelseId = UUID.randomUUID(),
                    tilstandsLogg = bekreftelse1.tilstandsLogg.copy(
                        siste = bekreftelseTilgjengeligSisteTilstandStatus,
                    ),
                    gjelderFra = bekreftelse1.gjelderFra + 1.days,
                    gjelderTil = bekreftelse1.gjelderTil + 1.days
                )
                val oppdatertTilstandMedToBekreftelserTilgjengelig = bekreftelseTilstand.copy(
                    bekreftelser = listOf(
                        bekreftelseTilstand.bekreftelser.first(),
                        bekreftelse2
                    )
                )
                stateStore.put(periode.id, oppdatertTilstandMedToBekreftelserTilgjengelig)

                testDriver.advanceWallClockTime(tilgjengeligOffset + 5.seconds)
                testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 5.seconds)
                testDriver.advanceWallClockTime(varselFoerGraceperiodeUtloept + 5.seconds)

                bekreftelseHendelseloggTopicOut.isEmpty shouldBe false

                val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                hendelser.filter { it.value is RegisterGracePeriodeUtloept }.size shouldBe 1
            }
        }
    }

    "BekreftelsePunctuator håndterer BekreftelsesMeldingMottatt hendelse for begge av to bekreftelser tilgjengelig når en bekreftelse har tilstand GracePeriodeVarselet" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (_, interval, _, tilgjengeligOffset, _) = bekreftelseKonfigurasjon
                val (_, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )
                periodeTopic.pipeInput(key, periode)

                testDriver.advanceWallClockTime(interval - tilgjengeligOffset + 5.seconds)

                val stateStore: BekreftelseTilstandStateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                val bekreftelseTilstand = stateStore.get(periode.id)
                val bekreftelse1 = bekreftelseTilstand.bekreftelser.first()
                val sisteTilstandsLogg = bekreftelse1.tilstandsLogg.siste.timestamp
                val gracePeriodeVarseletSisteTilstandStatus = GracePeriodeVarselet(sisteTilstandsLogg + 1.days)
                val bekreftelse2 = bekreftelse1.copy(
                    bekreftelseId = UUID.randomUUID(),
                    tilstandsLogg = bekreftelse1.tilstandsLogg.copy(
                        siste = gracePeriodeVarseletSisteTilstandStatus,
                    ),
                    gjelderFra = bekreftelse1.gjelderFra + 1.days,
                    gjelderTil = bekreftelse1.gjelderTil + 1.days
                )
                val oppdatertTilstandMedToBekreftelserTilgjengelig = bekreftelseTilstand.copy(
                    bekreftelser = listOf(
                        bekreftelseTilstand.bekreftelser.first(),
                        bekreftelse2
                    )
                )
                stateStore.put(periode.id, oppdatertTilstandMedToBekreftelserTilgjengelig)

                testDriver.advanceWallClockTime(tilgjengeligOffset + 5.seconds)

                val melding1 = no.nav.paw.bekreftelse.melding.v1.Bekreftelse(
                    periode.id,
                    Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                    bekreftelse1.bekreftelseId,
                    Svar(
                        Metadata(
                            Instant.now(), no.nav.paw.bekreftelse.melding.v1.vo.Bruker(
                                BrukerType.SLUTTBRUKER, identitetsnummer
                            ), "test", "test"
                        ),
                        bekreftelse1.gjelderFra,
                        bekreftelse1.gjelderTil,
                        true,
                        true
                    )
                )

                val melding2 = no.nav.paw.bekreftelse.melding.v1.Bekreftelse(
                    periode.id,
                    Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                    bekreftelse2.bekreftelseId,
                    Svar(
                        Metadata(
                            Instant.now(), no.nav.paw.bekreftelse.melding.v1.vo.Bruker(
                                BrukerType.SLUTTBRUKER, identitetsnummer
                            ), "test", "test"
                        ),
                        bekreftelse2.gjelderFra,
                        bekreftelse2.gjelderTil,
                        true,
                        true
                    )
                )

                bekreftelseTopic.pipeInput(key, melding1)
                bekreftelseTopic.pipeInput(key, melding2)

                testDriver.advanceWallClockTime(5.seconds)

                bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                hendelser.filter { it.value is BekreftelseMeldingMottatt }.size shouldBe 2
            }
        }
    }

    "BekreftelsePunctuator håndterer BekreftelsesMeldingMottatt hendelse for begge av to bekreftelser tilgjengelig" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (_, interval, _, tilgjengeligOffset, _) = bekreftelseKonfigurasjon
                val (_, key, periode) = periode(
                    identitetsnummer = identitetsnummer,
                    startetMetadata = metadata(tidspunkt = startTime)
                )
                periodeTopic.pipeInput(key, periode)

                testDriver.advanceWallClockTime(interval - tilgjengeligOffset + 5.seconds)

                val stateStore: BekreftelseTilstandStateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                val bekreftelseTilstand = stateStore.get(periode.id)
                val bekreftelse1 = bekreftelseTilstand.bekreftelser.first()
                val sisteTilstandsLogg = bekreftelse1.tilstandsLogg.siste.timestamp
                val bekreftelseTilgjengeligSisteTilstandStatus = KlarForUtfylling(sisteTilstandsLogg + 1.days)
                val bekreftelse2 = bekreftelse1.copy(
                    bekreftelseId = UUID.randomUUID(),
                    tilstandsLogg = bekreftelse1.tilstandsLogg.copy(
                        siste = bekreftelseTilgjengeligSisteTilstandStatus,
                    ),
                    gjelderFra = bekreftelse1.gjelderFra + 1.days,
                    gjelderTil = bekreftelse1.gjelderTil + 1.days
                )
                val oppdatertTilstandMedToBekreftelserTilgjengelig = bekreftelseTilstand.copy(
                    bekreftelser = listOf(
                        bekreftelseTilstand.bekreftelser.first(),
                        bekreftelse2
                    )
                )
                stateStore.put(periode.id, oppdatertTilstandMedToBekreftelserTilgjengelig)

                testDriver.advanceWallClockTime(tilgjengeligOffset + 5.seconds)

                val melding1 = no.nav.paw.bekreftelse.melding.v1.Bekreftelse(
                    periode.id,
                    Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                    bekreftelse1.bekreftelseId,
                    Svar(
                        Metadata(
                            Instant.now(), no.nav.paw.bekreftelse.melding.v1.vo.Bruker(
                                BrukerType.SLUTTBRUKER, identitetsnummer
                            ), "test", "test"
                        ),
                        bekreftelse1.gjelderFra,
                        bekreftelse1.gjelderTil,
                        true,
                        true
                    )
                )

                val melding2 = no.nav.paw.bekreftelse.melding.v1.Bekreftelse(
                    periode.id,
                    Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                    bekreftelse2.bekreftelseId,
                    Svar(
                        Metadata(
                            Instant.now(), no.nav.paw.bekreftelse.melding.v1.vo.Bruker(
                                BrukerType.SLUTTBRUKER, identitetsnummer
                            ), "test", "test"
                        ),
                        bekreftelse2.gjelderFra,
                        bekreftelse2.gjelderTil,
                        true,
                        true
                    )
                )

                bekreftelseTopic.pipeInput(key, melding1)
                bekreftelseTopic.pipeInput(key, melding2)

                testDriver.advanceWallClockTime(5.seconds)

                bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                val hendelser = bekreftelseHendelseloggTopicOut.readKeyValuesToList()
                hendelser.filter { it.value is BekreftelseMeldingMottatt }.size shouldBe 2
            }
        }
    }
})
