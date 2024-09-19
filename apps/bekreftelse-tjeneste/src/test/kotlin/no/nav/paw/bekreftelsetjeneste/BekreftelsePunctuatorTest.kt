package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.fristForNesteBekreftelse

class BekreftelsePunctuatorTest : FreeSpec({

    "Når KlarForUtfylling sendes BekreftelseTilgjengelig hendelse" - {
        with(ApplicationTestContext()){
            val (periode, kafkaKeyResponse) = periode(identitetsnummer = "12345678901")
            periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
            "Når perioden opprettes skal det ikke skje noe" {
                hendelseLoggTopic.isEmpty shouldBe true
            }
            "Etter 11 dager skal det sendes en BekreftelseTilgjengelig hendelse" {
                testDriver.advanceWallClockTime(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).plusDays(1))
                val stateStore:StateStore = testDriver.getKeyValueStore(applicationConfiguration.stateStoreName)
                stateStore.all().use {
                    it.forEach {
                        logger.info("key: ${it.key}, value: ${it.value}")
                    }
                }
                hendelseLoggTopic.isEmpty shouldBe false
                val kv = hendelseLoggTopic.readKeyValue()
                kv.key shouldBe kafkaKeyResponse.key
                with(kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()) {
                    periodeId shouldBe periode.id
                    arbeidssoekerId shouldBe kafkaKeyResponse.id
                    gjelderFra shouldBe periode.startet.tidspunkt
                    gjelderTil shouldBe fristForNesteBekreftelse(periode.startet.tidspunkt, BekreftelseConfig.bekreftelseInterval)
                }
            }
        }
    }

    /*for hver bekreftelse i "klar for utfylling" og gjelderTil passert, sett til "venter på svar" og send hendelse LeveringsFristUtloept

    for hver bekreftelse i "venter på svar" og ingen purring sendt og x tid passert siden frist, send RegisterGracePeriodeGjenstaaendeTid og sett purring sendt timestamp til now()

    for hver bekreftelse i "venter på svar" og grace periode utløpt, send RegisterGracePeriodeUtloept

    for hver periode hvis det er mindre enn x dager til den siste bekreftelse perioden utgår lag ny bekreftelse periode*/
})


