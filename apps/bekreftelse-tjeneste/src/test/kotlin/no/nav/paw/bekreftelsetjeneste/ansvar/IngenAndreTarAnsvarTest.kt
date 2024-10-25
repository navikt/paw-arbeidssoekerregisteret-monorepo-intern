package no.nav.paw.bekreftelsetjeneste.ansvar

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelsetjeneste.ApplicationTestContext
import no.nav.paw.test.days

class IngenAndreTarAnsvarTest : FreeSpec({
    with(ApplicationTestContext()) {
        val intervall = applicationConfig.bekreftelseKonfigurasjon.interval
        val grace = applicationConfig.bekreftelseKonfigurasjon.graceperiode
        with(kafkaKeyContext()) {
            "Applikasjonstest hvor ingen andre tar ansvar" - {
                "Bruker avslutter via rapportering" - {
                    val (id, key, periode) = periode(identitetsnummer = "12345678901")
                    periodeTopic.pipeInput(key, periode)
                    "Når perioden opprettes skal det ikke skje noe" {
                        bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
                    }
                    "Etter ${intervall.toDays()} dager skal en rapportering være tilgjengelig" {
                        testDriver.advanceWallClockTime(intervall)
                        bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                        val kv = bekreftelseHendelseloggTopicOut.readKeyValue()
                        kv.key shouldBe key
                        with(kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()) {
                            periodeId shouldBe periode.id
                            arbeidssoekerId shouldBe id
                            gjelderFra shouldBe periode.startet.tidspunkt
                        }
                    }
                    "Når rapporteringen ikke blir besvart innen fristen sendes det ut en melding" {
                        testDriver.advanceWallClockTime(grace + 1.days)
                        bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                        val kv = bekreftelseHendelseloggTopicOut.readKeyValue()
                        kv.key shouldBe key
                        with(kv.value.shouldBeInstanceOf<LeveringsfristUtloept>()) {
                            periodeId shouldBe periode.id
                            arbeidssoekerId shouldBe id
                        }
                    }
                }
            }
        }

    }
})

