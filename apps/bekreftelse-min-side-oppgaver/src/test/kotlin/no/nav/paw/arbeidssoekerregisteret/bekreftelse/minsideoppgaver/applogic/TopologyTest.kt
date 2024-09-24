package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseMottatt
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseTilgjengelig
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.periodeAvsluttet
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode


class TopologyTest: FreeSpec({
    "Verifiser standard applikasjonsflyt" {
        with(testContext()) {
            val periode = with(kafkaKeyContext) { periode() }
            val bekreftelseTilgjengelig = bekreftelseTilgjengelig(
                periodeId = periode.value.id
            )
            val bekreftelseMeldingMottatt = bekreftelseMottatt(
                periodeId = periode.value.id,
                bekreftelseId = bekreftelseTilgjengelig.bekreftelseId
            )
            val nyBekreftelseTilgjengelig = bekreftelseTilgjengelig(
                periodeId = periode.value.id
            )
            val endaEnnyBekreftelseTilgjengelig = bekreftelseTilgjengelig(
                periodeId = periode.value.id
            )
            val periodeAvsluttet = periodeAvsluttet(
                periodeId = periode.value.id
            )
            periodeTopic.pipeInput(periode.key, periode.value)
            bekreftelseHendelseTopic.pipeInput(periode.key, bekreftelseTilgjengelig)
            bekreftelseHendelseTopic.pipeInput(periode.key, bekreftelseMeldingMottatt)
            bekreftelseHendelseTopic.pipeInput(periode.key, nyBekreftelseTilgjengelig)
            bekreftelseHendelseTopic.pipeInput(periode.key, endaEnnyBekreftelseTilgjengelig)
            bekreftelseHendelseTopic.pipeInput(periode.key, periodeAvsluttet)

            tmsOppgaveTopic.isEmpty shouldBe false
            tmsOppgaveTopic.readKeyValue() should { (key, value) ->
                logger.info("key: $key, value: $value")
                key shouldBe bekreftelseTilgjengelig.bekreftelseId.toString()
                value shouldContain periode.value.identitetsnummer
                value shouldContain "opprett"
            }
            tmsOppgaveTopic.isEmpty shouldBe false
            tmsOppgaveTopic.readKeyValue() should { (key, value) ->
                logger.info("key: $key, value: $value")
                key shouldBe bekreftelseTilgjengelig.bekreftelseId.toString()
                value shouldContain "inaktiver"
            }
            tmsOppgaveTopic.isEmpty shouldBe false
            tmsOppgaveTopic.readKeyValue() should { (key, value) ->
                logger.info("key: $key, value: $value")
                key shouldBe nyBekreftelseTilgjengelig.bekreftelseId.toString()
                value shouldContain periode.value.identitetsnummer
                value shouldContain "opprett"
            }
            tmsOppgaveTopic.isEmpty shouldBe false
            tmsOppgaveTopic.readKeyValue() should { (key, value) ->
                logger.info("key: $key, value: $value")
                key shouldBe endaEnnyBekreftelseTilgjengelig.bekreftelseId.toString()
                value shouldContain periode.value.identitetsnummer
                value shouldContain "opprett"
            }
            tmsOppgaveTopic.isEmpty shouldBe false
            tmsOppgaveTopic.readKeyValuesToList() should { tmsMeldinger ->
                logger.info("Meldinger: {}", tmsMeldinger)
                tmsMeldinger.size shouldBe 2
                tmsMeldinger.firstOrNull { it.key == nyBekreftelseTilgjengelig.bekreftelseId.toString() }
                    .shouldNotBeNull()
                    .value shouldContain "inaktiver"
                tmsMeldinger.firstOrNull { it.key == endaEnnyBekreftelseTilgjengelig.bekreftelseId.toString() }
                    .shouldNotBeNull()
                    .value shouldContain "inaktiver"
            }
            tmsOppgaveTopic.isEmpty shouldBe true
        }
    }
})


