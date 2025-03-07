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

            "Verifiser håndtering av perioder" {
                periodeTopic.pipeInput(aapenPeriode2.asRecord(kafkaKey2))
                val periodeRows1 = periodeRepository.findAll()
                periodeRows1 shouldHaveSize 1
                val periodeRow1 = periodeRows1[0]
                periodeRow1.periodeId shouldBe aapenPeriode2.id
                periodeRow1.avsluttetTimestamp shouldBe null
                periodeRow1.updatedTimestamp shouldBe null

                periodeTopic.pipeInput(lukketPeriode2.asRecord(kafkaKey2))
                val periodeRows2 = periodeRepository.findAll()
                periodeRows2 shouldHaveSize 1
                val periodeRow2 = periodeRows2[0]
                periodeRow2.periodeId shouldBe aapenPeriode2.id
                periodeRow2.periodeId shouldBe lukketPeriode2.id
                periodeRow2.avsluttetTimestamp shouldNotBe null
                periodeRow2.updatedTimestamp shouldNotBe null

                periodeTopic.pipeInput(lukketPeriode3.asRecord(kafkaKey3))
                val periodeRows3 = periodeRepository.findAll()
                periodeRows3 shouldHaveSize 2
                val periodeRow3 = periodeRows3[1]
                periodeRow3.periodeId shouldBe aapenPeriode3.id
                periodeRow3.periodeId shouldBe lukketPeriode3.id
                periodeRow3.avsluttetTimestamp shouldNotBe null
                periodeRow3.updatedTimestamp shouldBe null

                periodeTopic.pipeInput(aapenPeriode3.asRecord(kafkaKey3))
                val periodeRows4 = periodeRepository.findAll()
                periodeRows4 shouldHaveSize 2
                val periodeRow4 = periodeRows4[1]
                periodeRow4.periodeId shouldBe aapenPeriode3.id
                periodeRow4.periodeId shouldBe lukketPeriode3.id
                periodeRow4.avsluttetTimestamp shouldNotBe null
                periodeRow4.updatedTimestamp shouldBe null
            }

            "Verifiser håndtering av bekreftelser".config(enabled = false) {
                periodeTopic.pipeInput(aapenPeriode1.asRecord(kafkaKey2))
                bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig1a.asRecord(kafkaKey1))
                bekreftelseVarselTopic.isEmpty shouldBe false
                bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1a.bekreftelseId.toString()
                    value.ident shouldBe aapenPeriode1.identitetsnummer
                    value.eventName shouldBe EventType.Opprett
                }
                bekreftelseVarselTopic.isEmpty shouldBe true
                varselRepository.countAll() shouldBe 1
                val varselRow1 = varselRepository.findByVarselId(bekreftelseTilgjengelig1a.bekreftelseId)
                varselRow1 shouldNotBe null
                varselRow1?.varselId shouldBe bekreftelseTilgjengelig1a.bekreftelseId
                varselRow1?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                varselRow1?.varselType shouldBe VarselType.OPPGAVE
                varselRow1?.hendelseName shouldBe VarselEventName.UKJENT
                varselRow1?.varselStatus shouldBe VarselStatus.UKJENT

                varselHendelseTopic.pipeInput(oppgaveVarselHendelse1a)
                varselHendelseTopic.pipeInput(oppgaveVarselHendelse1b)
                varselHendelseTopic.pipeInput(oppgaveVarselHendelse1c)
                varselHendelseTopic.pipeInput(oppgaveVarselHendelse1d)
                varselHendelseTopic.pipeInput(oppgaveVarselHendelse1e)
                varselRepository.countAll() shouldBe 1
                val varselRow2 = varselRepository.findByVarselId(bekreftelseTilgjengelig1a.bekreftelseId)
                varselRow2 shouldNotBe null
                varselRow2?.varselId shouldBe bekreftelseTilgjengelig1a.bekreftelseId
                varselRow2?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                varselRow2?.varselType shouldBe VarselType.OPPGAVE
                varselRow2?.hendelseName shouldBe VarselEventName.INAKTIVERT
                varselRow2?.varselStatus shouldBe VarselStatus.UKJENT

                bekreftelseHendelseTopic.pipeInput(bekreftelseMeldingMottatt1.asRecord(kafkaKey1))
                bekreftelseVarselTopic.isEmpty shouldBe false
                bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1a.bekreftelseId.toString()
                    value.eventName shouldBe EventType.Inaktiver
                }
                bekreftelseVarselTopic.isEmpty shouldBe true
                varselRepository.countAll() shouldBe 0
                val varselRow3 = varselRepository.findByVarselId(bekreftelseTilgjengelig1a.bekreftelseId)
                varselRow3 shouldBe null

                bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig1b.asRecord(kafkaKey1))
                bekreftelseVarselTopic.isEmpty shouldBe false
                bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1b.bekreftelseId.toString()
                    value.ident shouldBe aapenPeriode1.identitetsnummer
                    value.eventName shouldBe EventType.Opprett
                }
                bekreftelseVarselTopic.isEmpty shouldBe true
                varselRepository.countAll() shouldBe 1
                val varselRow4 = varselRepository.findByVarselId(bekreftelseTilgjengelig1b.bekreftelseId)
                varselRow4 shouldNotBe null
                varselRow4?.varselId shouldBe bekreftelseTilgjengelig1b.bekreftelseId
                varselRow4?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                varselRow4?.varselType shouldBe VarselType.OPPGAVE
                varselRow4?.hendelseName shouldBe VarselEventName.UKJENT
                varselRow4?.varselStatus shouldBe VarselStatus.UKJENT

                varselHendelseTopic.pipeInput(oppgaveVarselHendelse2a)
                varselHendelseTopic.pipeInput(oppgaveVarselHendelse2b)
                varselHendelseTopic.pipeInput(oppgaveVarselHendelse2c)
                varselHendelseTopic.pipeInput(oppgaveVarselHendelse2d)
                val varselRow5 = varselRepository.findByVarselId(bekreftelseTilgjengelig1b.bekreftelseId)
                varselRow5 shouldNotBe null
                varselRow5?.varselId shouldBe bekreftelseTilgjengelig1b.bekreftelseId
                varselRow5?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                varselRow5?.varselType shouldBe VarselType.OPPGAVE
                varselRow5?.hendelseName shouldBe VarselEventName.EKSTERN_STATUS_OPPDATERT
                varselRow5?.varselStatus shouldBe VarselStatus.FEILET

                bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig1c.asRecord(kafkaKey1))
                bekreftelseVarselTopic.isEmpty shouldBe false
                bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1c.bekreftelseId.toString()
                    value.ident shouldBe aapenPeriode1.identitetsnummer
                    value.eventName shouldBe EventType.Opprett
                }
                bekreftelseVarselTopic.isEmpty shouldBe true
                varselRepository.countAll() shouldBe 2
                val varselRow6 = varselRepository.findByVarselId(bekreftelseTilgjengelig1c.bekreftelseId)
                varselRow6 shouldNotBe null
                varselRow6?.varselId shouldBe bekreftelseTilgjengelig1c.bekreftelseId
                varselRow6?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                varselRow6?.varselType shouldBe VarselType.OPPGAVE
                varselRow6?.hendelseName shouldBe VarselEventName.UKJENT
                varselRow6?.varselStatus shouldBe VarselStatus.UKJENT

                bekreftelseHendelseTopic.pipeInput(periodeAvsluttet1.asRecord(kafkaKey1))
                bekreftelseVarselTopic.isEmpty shouldBe false
                bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1b.bekreftelseId.toString()
                    value.eventName shouldBe EventType.Inaktiver
                }
                bekreftelseVarselTopic.isEmpty shouldBe false
                bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                    val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                    key shouldBe bekreftelseTilgjengelig1c.bekreftelseId.toString()
                    value.eventName shouldBe EventType.Inaktiver
                }
                bekreftelseVarselTopic.isEmpty shouldBe true
                periodeRepository.countAll() shouldBe 1
                varselRepository.countAll() shouldBe 0
            }

            "Skal ignorere urelevante hendelser" {
                bekreftelseHendelseTopic.pipeInput(baOmAaAvsluttePeriode1.asRecord(kafkaKey1))
                bekreftelseHendelseTopic.pipeInput(leveringsfristUtloept1.asRecord(kafkaKey1))
                bekreftelseHendelseTopic.pipeInput(registerGracePeriodeUtloept1.asRecord(kafkaKey1))
                bekreftelseHendelseTopic.pipeInput(registerGracePeriodeUtloeptEtterEksternInnsamling1.asRecord(kafkaKey1))
                bekreftelseHendelseTopic.pipeInput(registerGracePeriodeGjenstaaendeTid1.asRecord(kafkaKey1))
                bekreftelseHendelseTopic.pipeInput(bekreftelsePaaVegneAvStartet1.asRecord(kafkaKey1))
                bekreftelseHendelseTopic.pipeInput(eksternGracePeriodeUtloept1.asRecord(kafkaKey1))

                bekreftelseVarselTopic.isEmpty shouldBe true
                periodeRepository.countAll() shouldBe 0
                varselRepository.countAll() shouldBe 0
            }
        }
    }
})


