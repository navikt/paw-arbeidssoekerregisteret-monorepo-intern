package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import java.time.Duration

@Ignored("Midlertidig disablet av Thomas")
class IngenAndreTarAnsvarTest : FreeSpec({
    with(ApplicationTestContext()) {
        with(kafkaKeyContext()) {
            "Applikasjons test hvor ingen andre tar ansvar" - {
                "Bruker avslutter via rapportering" - {
                    val (id, key, periode) = periode(identitetsnummer = "12345678901")
                    periodeTopic.pipeInput(key, periode)
                    "Nå perioden opprettes skal det ikke skje noe" {
                        bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
                    }
                    "Etter 13 dager skal en rapportering være tilgjengelig" {
                        testDriver.advanceWallClockTime(Duration.ofDays(13))
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
                        testDriver.advanceWallClockTime(Duration.ofDays(4))
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
