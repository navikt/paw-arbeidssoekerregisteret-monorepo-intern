package no.nav.paw.arbeidssoekerregisteret.topology

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssoekerregisteret.context.TestContext
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.tms.varsel.action.EventType
import no.nav.tms.varsel.action.InaktiverVarsel
import no.nav.tms.varsel.action.OpprettVarsel

class TopologyTest : FreeSpec({
    with(TestContext.build()) {
        with(TestData) {
            beforeTest {
                initDatabase()
            }

            "Skal ignorere urelevante hendelser".config(enabled = false) {
                bekreftelseHendelseTopic.pipeInput(
                    baOmAaAvsluttePeriode1.key,
                    baOmAaAvsluttePeriode1.value
                )
                bekreftelseHendelseTopic.pipeInput(
                    leveringsfristUtloept1.key,
                    leveringsfristUtloept1.value
                )
                bekreftelseHendelseTopic.pipeInput(
                    registerGracePeriodeUtloept1.key,
                    registerGracePeriodeUtloept1.value
                )
                bekreftelseHendelseTopic.pipeInput(
                    registerGracePeriodeUtloeptEtterEksternInnsamling1.key,
                    registerGracePeriodeUtloeptEtterEksternInnsamling1.value
                )
                bekreftelseHendelseTopic.pipeInput(
                    registerGracePeriodeGjenstaaendeTid1.key,
                    registerGracePeriodeGjenstaaendeTid1.value
                )
                bekreftelseHendelseTopic.pipeInput(
                    bekreftelsePaaVegneAvStartet1.key,
                    bekreftelsePaaVegneAvStartet1.value
                )
                bekreftelseHendelseTopic.pipeInput(
                    eksternGracePeriodeUtloept1.key,
                    eksternGracePeriodeUtloept1.value
                )

                tmsVarselTopic.isEmpty shouldBe true
                periodeRepository.countAll() shouldBe 0
                varselRepository.countAll() shouldBe 0
            }

            "Verifiser standard applikasjonsflyt".config(enabled = false) {
                periodeTopic.pipeInput(aapenPeriode1)
                periodeRepository.countAll() shouldBe 1
                varselRepository.countAll() shouldBe 0
                val periodeRow = periodeRepository.findByPeriodeId(aapenPeriode1.value.id)
                periodeRow shouldNotBe null
                periodeRow?.periodeId shouldBe aapenPeriode1.value.id

                bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig1a.key, bekreftelseTilgjengelig1a.value)
                tmsVarselTopic.isEmpty shouldBe false
                tmsVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1a.value.bekreftelseId.toString()
                    value.ident shouldBe aapenPeriode1.value.identitetsnummer
                    value.eventName shouldBe EventType.Opprett
                }
                tmsVarselTopic.isEmpty shouldBe true
                periodeRepository.countAll() shouldBe 1
                varselRepository.countAll() shouldBe 1
                val varselRow1 = varselRepository.findByVarselId(bekreftelseTilgjengelig1a.value.bekreftelseId)
                varselRow1 shouldNotBe null
                varselRow1?.varselId shouldBe bekreftelseTilgjengelig1a.value.bekreftelseId
                varselRow1?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                varselRow1?.varselType shouldBe VarselType.OPPGAVE
                varselRow1?.hendelseName shouldBe VarselEventName.UKJENT
                varselRow1?.varselStatus shouldBe VarselStatus.UKJENT

                tmsVarselHendelseTopic.pipeInput(oppgaveVarselHendelse1a)
                tmsVarselHendelseTopic.pipeInput(oppgaveVarselHendelse1b)
                tmsVarselHendelseTopic.pipeInput(oppgaveVarselHendelse1c)
                tmsVarselHendelseTopic.pipeInput(oppgaveVarselHendelse1d)
                tmsVarselHendelseTopic.pipeInput(oppgaveVarselHendelse1e)
                periodeRepository.countAll() shouldBe 1
                varselRepository.countAll() shouldBe 1
                val varselRow2 = varselRepository.findByVarselId(bekreftelseTilgjengelig1a.value.bekreftelseId)
                varselRow2 shouldNotBe null
                varselRow2?.varselId shouldBe bekreftelseTilgjengelig1a.value.bekreftelseId
                varselRow2?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                varselRow2?.varselType shouldBe VarselType.OPPGAVE
                varselRow2?.hendelseName shouldBe VarselEventName.INAKTIVERT
                varselRow2?.varselStatus shouldBe VarselStatus.UKJENT

                bekreftelseHendelseTopic.pipeInput(bekreftelseMeldingMottatt1.key, bekreftelseMeldingMottatt1.value)
                tmsVarselTopic.isEmpty shouldBe false
                tmsVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1a.value.bekreftelseId.toString()
                    value.eventName shouldBe EventType.Inaktiver
                }
                tmsVarselTopic.isEmpty shouldBe true
                periodeRepository.countAll() shouldBe 1
                varselRepository.countAll() shouldBe 0
                val varselRow3 = varselRepository.findByVarselId(bekreftelseTilgjengelig1a.value.bekreftelseId)
                varselRow3 shouldBe null

                bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig1b.key, bekreftelseTilgjengelig1b.value)
                tmsVarselTopic.isEmpty shouldBe false
                tmsVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1b.value.bekreftelseId.toString()
                    value.ident shouldBe aapenPeriode1.value.identitetsnummer
                    value.eventName shouldBe EventType.Opprett
                }
                tmsVarselTopic.isEmpty shouldBe true
                periodeRepository.countAll() shouldBe 1
                varselRepository.countAll() shouldBe 1
                val varselRow4 = varselRepository.findByVarselId(bekreftelseTilgjengelig1b.value.bekreftelseId)
                varselRow4 shouldNotBe null
                varselRow4?.varselId shouldBe bekreftelseTilgjengelig1b.value.bekreftelseId
                varselRow4?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                varselRow4?.varselType shouldBe VarselType.OPPGAVE
                varselRow4?.hendelseName shouldBe VarselEventName.UKJENT
                varselRow4?.varselStatus shouldBe VarselStatus.UKJENT

                tmsVarselHendelseTopic.pipeInput(oppgaveVarselHendelse2a)
                tmsVarselHendelseTopic.pipeInput(oppgaveVarselHendelse2b)
                tmsVarselHendelseTopic.pipeInput(oppgaveVarselHendelse2c)
                tmsVarselHendelseTopic.pipeInput(oppgaveVarselHendelse2d)
                val varselRow5 = varselRepository.findByVarselId(bekreftelseTilgjengelig1b.value.bekreftelseId)
                varselRow5 shouldNotBe null
                varselRow5?.varselId shouldBe bekreftelseTilgjengelig1b.value.bekreftelseId
                varselRow5?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                varselRow5?.varselType shouldBe VarselType.OPPGAVE
                varselRow5?.hendelseName shouldBe VarselEventName.EKSTERN_STATUS_OPPDATERT
                varselRow5?.varselStatus shouldBe VarselStatus.FEILET

                bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig1c.key, bekreftelseTilgjengelig1c.value)
                tmsVarselTopic.isEmpty shouldBe false
                tmsVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1c.value.bekreftelseId.toString()
                    value.ident shouldBe aapenPeriode1.value.identitetsnummer
                    value.eventName shouldBe EventType.Opprett
                }
                tmsVarselTopic.isEmpty shouldBe true
                periodeRepository.countAll() shouldBe 1
                varselRepository.countAll() shouldBe 2
                val varselRow6 = varselRepository.findByVarselId(bekreftelseTilgjengelig1c.value.bekreftelseId)
                varselRow6 shouldNotBe null
                varselRow6?.varselId shouldBe bekreftelseTilgjengelig1c.value.bekreftelseId
                varselRow6?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                varselRow6?.varselType shouldBe VarselType.OPPGAVE
                varselRow6?.hendelseName shouldBe VarselEventName.UKJENT
                varselRow6?.varselStatus shouldBe VarselStatus.UKJENT

                bekreftelseHendelseTopic.pipeInput(periodeAvsluttet1.key, periodeAvsluttet1.value)
                tmsVarselTopic.isEmpty shouldBe false
                tmsVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1b.value.bekreftelseId.toString()
                    value.eventName shouldBe EventType.Inaktiver
                }
                tmsVarselTopic.isEmpty shouldBe false
                tmsVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1c.value.bekreftelseId.toString()
                    value.eventName shouldBe EventType.Inaktiver
                }
                tmsVarselTopic.isEmpty shouldBe true
                periodeRepository.countAll() shouldBe 0
                varselRepository.countAll() shouldBe 0
            }

            "Skal motta perioder og bekreftelse-hendelser i korrekt rekkefølge".config(enabled = false) {
                val periodeRecords = aapenPeriodeRecords(10)
                periodeRecords.forEach { periodeRecord ->
                    periodeTopic.pipeInput(periodeRecord)
                    bekreftelseHendelseTopic.pipeInput(periodeRecord.bekreftelseTilgjengeligRecord())
                }

                val periodeRows = periodeRepository.findAll()
                val varselRows = varselRepository.findAll()
                logger.info("Perioder: {}", periodeRows)
                logger.info("Varsler: {}", varselRows)
                periodeRows shouldHaveSize 10
                varselRows shouldHaveSize 10
            }
        }
    }
})


