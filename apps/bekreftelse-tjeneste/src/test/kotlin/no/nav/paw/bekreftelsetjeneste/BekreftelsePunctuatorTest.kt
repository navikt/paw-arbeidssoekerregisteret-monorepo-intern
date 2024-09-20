package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjendstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseConfig
import java.time.Duration

class BekreftelsePunctuatorTest : FreeSpec({

    "BekreftelsePunctuator sender riktig hendelser" - {
        with(ApplicationTestContext()){
            val (periode, kafkaKeyResponse) = periode(identitetsnummer = "12345678901")
            periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
            "Når perioden opprettes skal det ikke skje noe" {
                hendelseLoggTopic.isEmpty shouldBe true
            }
            "Etter 11 dager skal det ha blitt sendt en BekreftelseTilgjengelig hendelse" {
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset))
                testDriver.advanceWallClockTime(Duration.ofHours(1))
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
                testDriver.advanceWallClockTime(Duration.ofHours(1))
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
                testDriver.advanceWallClockTime(Duration.ofHours(1))
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
                testDriver.advanceWallClockTime(Duration.ofHours(1))
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
                testDriver.advanceWallClockTime(Duration.ofDays(4))
                testDriver.advanceWallClockTime(Duration.ofHours(1))
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
})


