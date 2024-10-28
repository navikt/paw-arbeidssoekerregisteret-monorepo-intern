package no.nav.paw.bekreftelsetjeneste.paavegneav

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.stoppPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.startPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelsetjeneste.ApplicationTestContext
import no.nav.paw.test.assertEvent
import no.nav.paw.test.assertNoMessage
import no.nav.paw.test.days

class AndreTarAnsvarMenDetSendesAldriInnNoeTest: FreeSpec({
    with(ApplicationTestContext()) {
        val intervall = applicationConfig.bekreftelseKonfigurasjon.interval
        val grace = applicationConfig.bekreftelseKonfigurasjon.graceperiode
        with(kafkaKeyContext()) {
            "Applikasjonstest hvor noen tar ansvar rett etter at perioden er lest, men avslutter ansvar igjen før en eneste" +
                    " bekreftelse er levert" - {
                val (id, key, periode) = periode(identitetsnummer = "12345678902")
                "Når perioden opprettes skal det ikke skje noe" {
                    periodeTopic.pipeInput(key, periode)
                    bekreftelseHendelseloggTopicOut.assertNoMessage()
                }
                "Når andre tar ansvar sendes en 'AndreHarOvertattAnsvar' hendelse" {
                    val tarAnsvar = startPaaVegneAv(periodeId = periode.id)
                    bekreftelsePaaVegneAvTopic.pipeInput(key, tarAnsvar)
                    logger.info("Tar ansvar: $tarAnsvar")
                    bekreftelseHendelseloggTopicOut.assertEvent { hedelse: BekreftelsePaaVegneAvStartet ->
                        hedelse.periodeId shouldBe periode.id
                        hedelse.arbeidssoekerId shouldBe id
                    }
                    bekreftelseHendelseloggTopicOut.assertNoMessage()
                }
                "Når leveringsfristen utløper når andre har ansvar skjer det ingenting" {
                    testDriver.advanceWallClockTime(intervall + 1.days)
                    bekreftelseHendelseloggTopicOut.assertNoMessage()
                }
                "Når grace perioden utløpet når andre har ansvar skjer det ingenting" {
                    testDriver.advanceWallClockTime(grace + 1.days)
                    bekreftelseHendelseloggTopicOut.assertNoMessage()
                }
                "Når andre avslutter ansvar blir en ny bekreftelse tilgjengelig" {
                    bekreftelsePaaVegneAvTopic.pipeInput(key, stoppPaaVegneAv(periodeId = periode.id))
                    testDriver.advanceWallClockTime(1.days)
                    bekreftelseHendelseloggTopicOut.assertEvent { hendelse: BekreftelseTilgjengelig ->
                        hendelse.gjelderFra shouldBe periode.startet.tidspunkt
                    }
                }
            }
        }
    }
})