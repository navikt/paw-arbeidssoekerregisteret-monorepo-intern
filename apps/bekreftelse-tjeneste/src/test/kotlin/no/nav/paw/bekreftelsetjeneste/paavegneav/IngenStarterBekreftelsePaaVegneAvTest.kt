package no.nav.paw.bekreftelsetjeneste.paavegneav

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelsetjeneste.ApplicationTestContext
import no.nav.paw.test.days
import java.time.Instant

class IngenStarterBekreftelsePaaVegneAvTest : FreeSpec({
    val startTime = Instant.parse("2024-01-02T08:00:00Z")
    with(ApplicationTestContext(initialWallClockTime = startTime)) {
        val grace = applicationConfig.bekreftelseKonfigurasjon.graceperiode
        with(kafkaKeyContext()) {
            "Applikasjonstest hvor ingen andre starter bekreftelsePaaVegneAv" - {
                "Bruker avslutter via bekreftelse" - {
                    val (id, key, periode) = periode(identitetsnummer = "12345678901", startetMetadata = metadata(tidspunkt = startTime))
                    periodeTopic.pipeInput(key, periode)
                    "Når perioden opprettes skal det ikke skje noe" {
                        bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
                    }
                    "Etter 18 dager fra en tirsdag skal en bekreftelse være tilgjengelig" {
                        testDriver.advanceWallClockTime(18.days)
                        bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                        val kv = bekreftelseHendelseloggTopicOut.readKeyValue()
                        kv.key shouldBe key
                        with(kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()) {
                            periodeId shouldBe periode.id
                            arbeidssoekerId shouldBe id
                            gjelderFra shouldBe periode.startet.tidspunkt
                        }
                    }
                    "Når bekreftelse ikke blir besvart innen fristen sendes det ut en melding" {
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

