package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjendstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.PeriodeInfo
import no.nav.paw.bekreftelsetjeneste.tilstand.Tilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.fristForNesteBekreftelse
import java.time.Duration
import java.time.Instant

class BekreftelsePunctuatorTest : FreeSpec({

    val startTime = Instant.ofEpochMilli(1704185347) // 01-01-2024 - 08:49:07
    val identitetsnummer = "12345678901"

    "BekreftelsePunctuator sender riktig hendelser i rekkefølge" - {
        with(ApplicationTestContext(initialWallClockTime = startTime)){
            val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)
            periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
            "Når perioden opprettes skal det opprettes en intern tilstand med en bekreftelse" {
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                hendelseLoggTopic.isEmpty shouldBe true
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
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopic.isEmpty shouldBe false
                val hendelser = hendelseLoggTopic.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 1
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()
            }
            "Etter 14 dager skal det ha blitt sendt en LeveringsFristUtloept hendelse" {
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseTilgjengeligOffset)
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopic.isEmpty shouldBe false
                val hendelser = hendelseLoggTopic.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 1
                val hendelseLast = hendelser.last()
                hendelseLast.key shouldBe kafkaKeyResponse.key
                hendelseLast.value.shouldBeInstanceOf<LeveringsfristUtloept>()
            }
            "Etter 17,5 dager uten svar skal det ha blitt sendt en RegisterGracePeriodeGjenstaaendeTid hendelse" {
                testDriver.advanceWallClockTime(BekreftelseConfig.varselFoerGracePeriodeUtloept)
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopic.isEmpty shouldBe false
                val hendelser = hendelseLoggTopic.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 1
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<RegisterGracePeriodeGjendstaaendeTid>()
            }
            "Etter 21 dager uten svar skal det ha blitt sendt en RegisterGracePeriodeUtloept hendelse" {
                testDriver.advanceWallClockTime(BekreftelseConfig.varselFoerGracePeriodeUtloept)
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopic.isEmpty shouldBe false
                val hendelser = hendelseLoggTopic.readKeyValuesToList()
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
                testDriver.advanceWallClockTime(Duration.ofSeconds(5))
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopic.isEmpty shouldBe false
                val hendelser = hendelseLoggTopic.readKeyValuesToList()
                logger.info("hendelser: $hendelser")
                hendelser.size shouldBe 1
                val kv = hendelser.last()
                kv.key shouldBe kafkaKeyResponse.key
                kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()
            }
        }
    }
    // TODO: Lag individuelle tester for hver hendelse, for BekreftelseMeldingMottatt og BaOmAaAvsluttePeriode
    /*"BekreftelsePunctuator håndterer BekreftelseMeldingMotatt og BaOmAaAvsluttePeriode hendelser" - {
        with(ApplicationTestContext(initialWallClockTime = startTime)){
            val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)

        }
    }*/
})


