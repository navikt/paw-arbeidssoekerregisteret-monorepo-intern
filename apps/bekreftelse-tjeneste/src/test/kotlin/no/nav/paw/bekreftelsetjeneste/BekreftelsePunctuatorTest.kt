package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjendstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.PeriodeInfo
import no.nav.paw.bekreftelsetjeneste.tilstand.Tilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.fristForNesteBekreftelse
import java.time.Duration
import java.time.Instant

class BekreftelsePunctuatorTest : FreeSpec({

    val startTime = Instant.ofEpochMilli(1704185347000) // 01-01-2024 - 08:49:07
    val identitetsnummer = "12345678901"

    "BekreftelsePunctuator sender riktig hendelser i rekkefølge" - {
        with(ApplicationTestContext(initialWallClockTime = startTime)){
            val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)
            periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
            "Når perioden opprettes skal det opprettes en intern tilstand med en bekreftelse" {
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                hendelseLoggTopicOut.isEmpty shouldBe true
                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                val currentState = stateStore.get(periode.id)
                currentState shouldBe InternTilstand(
                    periode = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = kafkaKeyResponse.id,
                        recordKey = kafkaKeyResponse.key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    ),
                    bekreftelser = listOf(
                        Bekreftelse(
                            tilstand = Tilstand.IkkeKlarForUtfylling,
                            tilgjengeliggjort = null,
                            fristUtloept = null,
                            sisteVarselOmGjenstaaendeGraceTid = null,
                            bekreftelseId = currentState.bekreftelser.first().bekreftelseId,
                            gjelderFra = periode.startet.tidspunkt,
                            gjelderTil = fristForNesteBekreftelse(periode.startet.tidspunkt, BekreftelseConfig.bekreftelseInterval)
                        )
                    )
                )

            }
            "Etter 11 dager skal det ha blitt sendt en BekreftelseTilgjengelig hendelse" {
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset))
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopicOut.isEmpty shouldBe false
                val hendelser = hendelseLoggTopicOut.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 1
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()
            }
            "Etter 14 dager skal det ha blitt sendt en LeveringsFristUtloept hendelse" {
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseTilgjengeligOffset.plusSeconds(5))
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopicOut.isEmpty shouldBe false
                val hendelser = hendelseLoggTopicOut.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 1
                val hendelseLast = hendelser.last()
                hendelseLast.key shouldBe kafkaKeyResponse.key
                hendelseLast.value.shouldBeInstanceOf<LeveringsfristUtloept>()
            }
            "Etter 17,5 dager uten svar skal det ha blitt sendt en RegisterGracePeriodeGjenstaaendeTid hendelse" {
                testDriver.advanceWallClockTime(BekreftelseConfig.varselFoerGracePeriodeUtloept.plusSeconds(5))
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopicOut.isEmpty shouldBe false
                val hendelser = hendelseLoggTopicOut.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 1
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<RegisterGracePeriodeGjendstaaendeTid>()
            }
            "Etter 21 dager uten svar skal det ha blitt sendt en RegisterGracePeriodeUtloept hendelse" {
                testDriver.advanceWallClockTime(BekreftelseConfig.varselFoerGracePeriodeUtloept.plusSeconds(5))
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopicOut.isEmpty shouldBe false
                val hendelser = hendelseLoggTopicOut.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 1
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<RegisterGracePeriodeUtloept>()
            }
            "Etter 25 dager skal det ha blitt sendt en BekreftelseTilgjengelig hendelse" {
                testDriver.advanceWallClockTime(
                    Duration.between(
                        startTime.plus(BekreftelseConfig.bekreftelseInterval).plus(BekreftelseConfig.gracePeriode),
                        startTime.plus(BekreftelseConfig.bekreftelseInterval).plus(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset))
                    )
                )
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopicOut.isEmpty shouldBe false
                val hendelser = hendelseLoggTopicOut.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 1
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()
            }
        }
    }
    "BekreftelsePunctuator håndterer BekreftelseMeldingMotatt hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)){
            val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)

            periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
            testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusSeconds(5))
            val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
            val currentState = stateStore.get(periode.id)
            bekreftelseTopic.pipeInput(kafkaKeyResponse.key,
                no.nav.paw.bekreftelse.melding.v1.Bekreftelse(
                    periode.id,
                    "paw",
                    currentState.bekreftelser.first().bekreftelseId,
                    Svar(
                        Metadata(
                            Instant.now(),
                            no.nav.paw.bekreftelse.melding.v1.vo.Bruker(
                                BrukerType.SLUTTBRUKER,
                                "12345678901"
                            ),
                            "test",
                            "test"
                        ),
                        periode.startet.tidspunkt,
                        fristForNesteBekreftelse(periode.startet.tidspunkt, BekreftelseConfig.bekreftelseInterval),
                        true,
                        true
                    )
                )
            )
            testDriver.advanceWallClockTime(Duration.ofSeconds(5))
            testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseTilgjengeligOffset.plus(BekreftelseConfig.gracePeriode))
            val hendelseLoggOutput = hendelseLoggTopicOut.readKeyValuesToList()
            logger.info("hendelseOutput: $hendelseLoggOutput")
            stateStore.all().use {
                it.forEach {
                    logger.info("key: ${it.key}, value: ${it.value}")
                }
            }
            hendelseLoggOutput.size shouldBe 2
            hendelseLoggOutput.filter { it.value is BekreftelseTilgjengelig }.size shouldBe 1
            hendelseLoggOutput.filter { it.value is BekreftelseMeldingMottatt }.size shouldBe 1
        }
    }
    "BekreftelsePunctuator håndterer BaOmAaAvslutePeriode hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)

            periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
            testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusSeconds(5))
            val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
            val currentState = stateStore.get(periode.id)
            bekreftelseTopic.pipeInput(
                kafkaKeyResponse.key,
                no.nav.paw.bekreftelse.melding.v1.Bekreftelse(
                    periode.id,
                    "paw",
                    currentState.bekreftelser.first().bekreftelseId,
                    Svar(
                        Metadata(
                            Instant.now(),
                            no.nav.paw.bekreftelse.melding.v1.vo.Bruker(
                                BrukerType.SLUTTBRUKER,
                                "12345678901"
                            ),
                            "test",
                            "test"
                        ),
                        periode.startet.tidspunkt,
                        fristForNesteBekreftelse(periode.startet.tidspunkt, BekreftelseConfig.bekreftelseInterval),
                        true,
                        false
                    )
                )
            )
            testDriver.advanceWallClockTime(Duration.ofSeconds(5))
            testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseTilgjengeligOffset.plus(BekreftelseConfig.gracePeriode))
            val hendelseLoggOutput = hendelseLoggTopicOut.readKeyValuesToList()
            logger.info("hendelseOutput: $hendelseLoggOutput")
            stateStore.all().use {
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
    "BekreftelsePunctuator setter riktig tilstand og sender riktig hendelse for: " - {
        "IkkeKlarForUtfylling" {
            with(ApplicationTestContext(initialWallClockTime = startTime)) {
                val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)
                periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                val currentState = stateStore.get(periode.id)
                currentState shouldBe InternTilstand(
                    periode = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = kafkaKeyResponse.id,
                        recordKey = kafkaKeyResponse.key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    ),
                    bekreftelser = listOf(
                        Bekreftelse(
                            tilstand = Tilstand.IkkeKlarForUtfylling,
                            tilgjengeliggjort = null,
                            fristUtloept = null,
                            sisteVarselOmGjenstaaendeGraceTid = null,
                            bekreftelseId = currentState.bekreftelser.first().bekreftelseId,
                            gjelderFra = periode.startet.tidspunkt,
                            gjelderTil = fristForNesteBekreftelse(periode.startet.tidspunkt, BekreftelseConfig.bekreftelseInterval)
                        )
                    )
                )
                hendelseLoggTopicOut.isEmpty shouldBe true
            }
        }
        "KlarForUtfylling og BekreftelseTilgjengelig" {
            with(ApplicationTestContext(initialWallClockTime = startTime)) {
                val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)
                periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusSeconds(5))
                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                val currentState = stateStore.get(periode.id)
                currentState shouldBe InternTilstand(
                    periode = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = kafkaKeyResponse.id,
                        recordKey = kafkaKeyResponse.key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    ),
                    bekreftelser = listOf(
                        Bekreftelse(
                            tilstand = Tilstand.KlarForUtfylling,
                            tilgjengeliggjort = startTime.plus(BekreftelseConfig.bekreftelseInterval).minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusSeconds(5),
                            fristUtloept = null,
                            sisteVarselOmGjenstaaendeGraceTid = null,
                            bekreftelseId = currentState.bekreftelser.first().bekreftelseId,
                            gjelderFra = periode.startet.tidspunkt,
                            gjelderTil = fristForNesteBekreftelse(periode.startet.tidspunkt, BekreftelseConfig.bekreftelseInterval)
                        )
                    )
                )
                hendelseLoggTopicOut.isEmpty shouldBe false
                val hendelser = hendelseLoggTopicOut.readKeyValuesToList()
                hendelser.size shouldBe 1
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()
            }
        }
        "VenterSvar og LeveringsfristUtloept" {
            with(ApplicationTestContext(initialWallClockTime = startTime)) {
                val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)
                periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
                // Spoler frem til BekreftelseTilgjengelig
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusSeconds(5))
                // Spoler frem til LeveringsfristUtloept
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseTilgjengeligOffset.plusSeconds(5))
                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                val currentState = stateStore.get(periode.id)
                currentState shouldBe InternTilstand(
                    periode = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = kafkaKeyResponse.id,
                        recordKey = kafkaKeyResponse.key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    ),
                    bekreftelser = listOf(
                        Bekreftelse(
                            tilstand = Tilstand.VenterSvar,
                            tilgjengeliggjort = startTime.plus(BekreftelseConfig.bekreftelseInterval).minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusSeconds(5),
                            fristUtloept = startTime.plus(BekreftelseConfig.bekreftelseInterval).plusSeconds(10),
                            sisteVarselOmGjenstaaendeGraceTid = null,
                            bekreftelseId = currentState.bekreftelser.first().bekreftelseId,
                            gjelderFra = periode.startet.tidspunkt,
                            gjelderTil = fristForNesteBekreftelse(periode.startet.tidspunkt, BekreftelseConfig.bekreftelseInterval)
                        )
                    )
                )
                hendelseLoggTopicOut.isEmpty shouldBe false
                val hendelser = hendelseLoggTopicOut.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 2
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<LeveringsfristUtloept>()
            }
        }
        "VenterSvar og RegisterGracePeriodeGjenstaaende" {
            with(ApplicationTestContext(initialWallClockTime = startTime)) {
                val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)
                periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
                // Spoler frem til BekreftelseTilgjengelig
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusSeconds(5))
                // Spoler frem til LeveringsfristUtloept
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseTilgjengeligOffset.plusSeconds(5))
                // Spoler frem til RegisterGracePeriodeGjenstaaende
                testDriver.advanceWallClockTime(BekreftelseConfig.varselFoerGracePeriodeUtloept.plusSeconds(5))
                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                val currentState = stateStore.get(periode.id)
                currentState shouldBe InternTilstand(
                    periode = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = kafkaKeyResponse.id,
                        recordKey = kafkaKeyResponse.key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    ),
                    bekreftelser = listOf(
                        Bekreftelse(
                            tilstand = Tilstand.VenterSvar,
                            tilgjengeliggjort = startTime.plus(BekreftelseConfig.bekreftelseInterval).minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusSeconds(5),
                            fristUtloept = startTime.plus(BekreftelseConfig.bekreftelseInterval).plusSeconds(10),
                            sisteVarselOmGjenstaaendeGraceTid = startTime.plus(BekreftelseConfig.bekreftelseInterval).plus(BekreftelseConfig.varselFoerGracePeriodeUtloept).plusSeconds(15),
                            bekreftelseId = currentState.bekreftelser.first().bekreftelseId,
                            gjelderFra = periode.startet.tidspunkt,
                            gjelderTil = fristForNesteBekreftelse(periode.startet.tidspunkt, BekreftelseConfig.bekreftelseInterval)
                        )
                    )
                )
                hendelseLoggTopicOut.isEmpty shouldBe false
                val hendelser = hendelseLoggTopicOut.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 3
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<RegisterGracePeriodeGjendstaaendeTid>()
            }
        }
        "GracePeriodeUtloept og RegisterGracePeriodeUtloept" {
            with(ApplicationTestContext(initialWallClockTime = startTime)) {
                val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)
                periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
                // Spoler frem til BekreftelseTilgjengelig
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusSeconds(5))
                // Spoler frem til LeveringsfristUtloept
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseTilgjengeligOffset.plusSeconds(5))
                // Spoler frem til RegisterGracePeriodeGjenstaaendeTid
                testDriver.advanceWallClockTime(BekreftelseConfig.varselFoerGracePeriodeUtloept.plusSeconds(5))
                // Spoler frem til RegisterGracePeriodeUtloept
                testDriver.advanceWallClockTime(BekreftelseConfig.varselFoerGracePeriodeUtloept.plusSeconds(5))
                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                val currentState = stateStore.get(periode.id)
                currentState shouldBe InternTilstand(
                    periode = PeriodeInfo(
                        periodeId = periode.id,
                        identitetsnummer = periode.identitetsnummer,
                        arbeidsoekerId = kafkaKeyResponse.id,
                        recordKey = kafkaKeyResponse.key,
                        startet = periode.startet.tidspunkt,
                        avsluttet = periode.avsluttet?.tidspunkt
                    ),
                    bekreftelser = listOf(
                        Bekreftelse(
                            tilstand = Tilstand.GracePeriodeUtloept,
                            tilgjengeliggjort = startTime.plus(BekreftelseConfig.bekreftelseInterval).minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusSeconds(5),
                            fristUtloept = startTime.plus(BekreftelseConfig.bekreftelseInterval).plusSeconds(10),
                            sisteVarselOmGjenstaaendeGraceTid = startTime.plus(BekreftelseConfig.bekreftelseInterval).plus(BekreftelseConfig.varselFoerGracePeriodeUtloept).plusSeconds(15),
                            bekreftelseId = currentState.bekreftelser.first().bekreftelseId,
                            gjelderFra = periode.startet.tidspunkt,
                            gjelderTil = fristForNesteBekreftelse(periode.startet.tidspunkt, BekreftelseConfig.bekreftelseInterval)
                        )
                    )
                )
                hendelseLoggTopicOut.isEmpty shouldBe false
                val hendelser = hendelseLoggTopicOut.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 4
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<RegisterGracePeriodeUtloept>()
            }
        }
    }
})


