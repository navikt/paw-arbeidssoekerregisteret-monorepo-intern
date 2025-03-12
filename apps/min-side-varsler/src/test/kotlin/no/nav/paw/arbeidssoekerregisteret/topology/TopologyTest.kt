package no.nav.paw.arbeidssoekerregisteret.topology

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import no.nav.paw.arbeidssoekerregisteret.context.KafkaTestContext
import no.nav.paw.arbeidssoekerregisteret.exception.PeriodeIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.tms.varsel.action.EventType
import no.nav.tms.varsel.action.InaktiverVarsel
import no.nav.tms.varsel.action.OpprettVarsel
import no.nav.tms.varsel.action.Varseltype
import org.apache.kafka.streams.errors.StreamsException
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

class TopologyTest : FreeSpec({
    with(KafkaTestContext.buildWithH2()) {
        with(TestData) {
            beforeTest { initDatabase() }

            "Verifiser håndtering av perioder" - {
                "Normal rekkefølge med åpen før lukket periode" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    periodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    val periodeRows1 = periodeRepository.findAll()
                    periodeRows1 shouldHaveSize 1
                    val periodeRow1 = periodeRows1[0]
                    periodeRow1.periodeId shouldBe aapenPeriode.id
                    periodeRow1.avsluttetTimestamp shouldBe null
                    periodeRow1.updatedTimestamp shouldBe null
                    periodeVarselTopic.isEmpty shouldBe true
                    varselRepository.findAll() shouldHaveSize 0

                    val lukketPeriode = lukketPeriode(
                        id = aapenPeriode.id,
                        identitetsnummer = aapenPeriode.identitetsnummer,
                        startet = aapenPeriode.startet
                    )
                    periodeTopic.pipeInput(lukketPeriode.asRecord(key))
                    periodeVarselTopic.isEmpty shouldBe false
                    periodeVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe lukketPeriode.id.toString()
                        value.type shouldBe Varseltype.Beskjed
                        value.ident shouldBe lukketPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                    }
                    periodeVarselTopic.isEmpty shouldBe true
                    val periodeRows2 = periodeRepository.findAll()
                    periodeRows2 shouldHaveSize 1
                    val periodeRow2 = periodeRows2[0]
                    periodeRow2.periodeId shouldBe aapenPeriode.id
                    periodeRow2.periodeId shouldBe lukketPeriode.id
                    periodeRow2.avsluttetTimestamp shouldNotBe null
                    periodeRow2.updatedTimestamp shouldNotBe null
                    varselRepository.findAll() shouldHaveSize 1
                    val varselRow1 = varselRepository.findByVarselId(lukketPeriode.id)
                    varselRow1 shouldNotBe null
                    varselRow1?.varselId shouldBe lukketPeriode.id
                    varselRow1?.varselKilde shouldBe VarselKilde.PERIODE_AVSLUTTET
                    varselRow1?.varselType shouldBe VarselType.BESKJED
                    varselRow1?.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow1?.varselStatus shouldBe VarselStatus.UKJENT

                    periodeTopic.pipeInput(lukketPeriode.asRecord(key))
                    periodeVarselTopic.isEmpty shouldBe true
                }

                "Motsatt rekkefølge med lukket før åpen periode" {
                    val key = Random.nextLong()
                    val lukketPeriode = lukketPeriode()
                    periodeTopic.pipeInput(lukketPeriode.asRecord(key))
                    periodeVarselTopic.isEmpty shouldBe false
                    periodeVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe lukketPeriode.id.toString()
                        value.type shouldBe Varseltype.Beskjed
                        value.ident shouldBe lukketPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                    }
                    periodeVarselTopic.isEmpty shouldBe true
                    val periodeRows3 = periodeRepository.findAll()
                    periodeRows3 shouldHaveSize 1
                    val periodeRow3 = periodeRows3[0]
                    periodeRow3.periodeId shouldBe lukketPeriode.id
                    periodeRow3.avsluttetTimestamp shouldNotBe null
                    periodeRow3.updatedTimestamp shouldBe null
                    varselRepository.findAll() shouldHaveSize 1
                    val varselRow2 = varselRepository.findByVarselId(lukketPeriode.id)
                    varselRow2 shouldNotBe null
                    varselRow2?.varselId shouldBe lukketPeriode.id
                    varselRow2?.varselKilde shouldBe VarselKilde.PERIODE_AVSLUTTET
                    varselRow2?.varselType shouldBe VarselType.BESKJED
                    varselRow2?.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow2?.varselStatus shouldBe VarselStatus.UKJENT

                    val aapenPeriode = aapenPeriode(
                        id = lukketPeriode.id,
                        identitetsnummer = lukketPeriode.identitetsnummer,
                        startet = lukketPeriode.startet
                    )
                    periodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    periodeVarselTopic.isEmpty shouldBe true
                    val periodeRows4 = periodeRepository.findAll()
                    periodeRows4 shouldHaveSize 1
                    val periodeRow4 = periodeRows4[0]
                    periodeRow4.periodeId shouldBe aapenPeriode.id
                    periodeRow4.periodeId shouldBe lukketPeriode.id
                    periodeRow4.avsluttetTimestamp shouldNotBe null
                    periodeRow4.updatedTimestamp shouldBe null
                }
            }

            "Verifiser håndtering av bekreftelser" - {
                "Bekreftelse tilgjengelig så bekreftelse motatt" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    bekreftelsePeriodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    val bekreftelseTilgjengelig = bekreftelseTilgjengelig(
                        periodeId = aapenPeriode.id,
                        hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(10))
                    )
                    bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig.asRecord(key))
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe bekreftelseTilgjengelig.bekreftelseId.toString()
                        value.type shouldBe Varseltype.Oppgave
                        value.ident shouldBe aapenPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true
                    varselRepository.findAll() shouldHaveSize 1
                    val varselRow1 = varselRepository.findByVarselId(bekreftelseTilgjengelig.bekreftelseId)
                    varselRow1 shouldNotBe null
                    varselRow1?.varselId shouldBe bekreftelseTilgjengelig.bekreftelseId
                    varselRow1?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow1?.varselType shouldBe VarselType.OPPGAVE
                    varselRow1?.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow1?.varselStatus shouldBe VarselStatus.UKJENT

                    val oppgaveVarselHendelse1 = varselHendelse(
                        eventName = VarselEventName.OPPRETTET,
                        varselId = bekreftelseTilgjengelig.bekreftelseId.toString(),
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(5))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse1)
                    val oppgaveVarselHendelse2 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = bekreftelseTilgjengelig.bekreftelseId.toString(),
                        status = VarselStatus.BESTILT,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(4))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse2)
                    val oppgaveVarselHendelse3 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = bekreftelseTilgjengelig.bekreftelseId.toString(),
                        status = VarselStatus.SENDT,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(3))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse3)
                    val oppgaveVarselHendelse4 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = bekreftelseTilgjengelig.bekreftelseId.toString(),
                        status = VarselStatus.FERDIGSTILT,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(2))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse4)
                    val oppgaveVarselHendelse5 = varselHendelse(
                        eventName = VarselEventName.INAKTIVERT,
                        varselId = bekreftelseTilgjengelig.bekreftelseId.toString(),
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(1))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse5)
                    varselRepository.findAll() shouldHaveSize 1
                    val varselRow2 = varselRepository.findByVarselId(bekreftelseTilgjengelig.bekreftelseId)
                    varselRow2 shouldNotBe null
                    varselRow2?.varselId shouldBe bekreftelseTilgjengelig.bekreftelseId
                    varselRow2?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow2?.varselType shouldBe VarselType.OPPGAVE
                    varselRow2?.hendelseName shouldBe VarselEventName.INAKTIVERT
                    varselRow2?.varselStatus shouldBe VarselStatus.UKJENT

                    val bekreftelseMeldingMottatt = bekreftelseMeldingMottatt(
                        periodeId = aapenPeriode.id,
                        bekreftelseId = bekreftelseTilgjengelig.bekreftelseId,
                        arbeidssoekerId = bekreftelseTilgjengelig.arbeidssoekerId,
                    )
                    bekreftelseHendelseTopic.pipeInput(bekreftelseMeldingMottatt.asRecord(key))
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                        key shouldBe bekreftelseTilgjengelig.bekreftelseId.toString()
                        value.eventName shouldBe EventType.Inaktiver
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true
                    varselRepository.findAll() shouldHaveSize 0
                    val varselRow3 = varselRepository.findByVarselId(bekreftelseTilgjengelig.bekreftelseId)
                    varselRow3 shouldBe null
                }

                "To bekreftelser tilgjengelig så periode avsluttet" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    bekreftelsePeriodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    val bekreftelseTilgjengelig1 = bekreftelseTilgjengelig(
                        periodeId = aapenPeriode.id,
                        hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(20))
                    )
                    bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig1.asRecord(key))
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe bekreftelseTilgjengelig1.bekreftelseId.toString()
                        value.type shouldBe Varseltype.Oppgave
                        value.ident shouldBe aapenPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true
                    varselRepository.findAll() shouldHaveSize 1
                    val varselRow4 = varselRepository.findByVarselId(bekreftelseTilgjengelig1.bekreftelseId)
                    varselRow4 shouldNotBe null
                    varselRow4?.varselId shouldBe bekreftelseTilgjengelig1.bekreftelseId
                    varselRow4?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow4?.varselType shouldBe VarselType.OPPGAVE
                    varselRow4?.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow4?.varselStatus shouldBe VarselStatus.UKJENT

                    val oppgaveVarselHendelse1 = varselHendelse(
                        eventName = VarselEventName.OPPRETTET,
                        varselId = bekreftelseTilgjengelig1.bekreftelseId.toString(),
                        status = VarselStatus.VENTER,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(3))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse1)
                    val oppgaveVarselHendelse2 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = bekreftelseTilgjengelig1.bekreftelseId.toString(),
                        status = VarselStatus.BESTILT,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(2))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse2)
                    val oppgaveVarselHendelse3 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = bekreftelseTilgjengelig1.bekreftelseId.toString(),
                        status = VarselStatus.FEILET,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(1))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse3)
                    val oppgaveVarselHendelse4 = varselHendelse(
                        eventName = VarselEventName.INAKTIVERT,
                        varselId = bekreftelseTilgjengelig1.bekreftelseId.toString(),
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(10))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse4)
                    val varselRow5 = varselRepository.findByVarselId(bekreftelseTilgjengelig1.bekreftelseId)
                    varselRow5 shouldNotBe null
                    varselRow5?.varselId shouldBe bekreftelseTilgjengelig1.bekreftelseId
                    varselRow5?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow5?.varselType shouldBe VarselType.OPPGAVE
                    varselRow5?.hendelseName shouldBe VarselEventName.EKSTERN_STATUS_OPPDATERT
                    varselRow5?.varselStatus shouldBe VarselStatus.FEILET

                    val bekreftelseTilgjengelig2 = bekreftelseTilgjengelig(
                        periodeId = aapenPeriode.id,
                        hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(10))
                    )
                    bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig2.asRecord(key))
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe bekreftelseTilgjengelig2.bekreftelseId.toString()
                        value.type shouldBe Varseltype.Oppgave
                        value.ident shouldBe aapenPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true
                    varselRepository.findAll() shouldHaveSize 2
                    val varselRow6 = varselRepository.findByVarselId(bekreftelseTilgjengelig2.bekreftelseId)
                    varselRow6 shouldNotBe null
                    varselRow6?.varselId shouldBe bekreftelseTilgjengelig2.bekreftelseId
                    varselRow6?.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow6?.varselType shouldBe VarselType.OPPGAVE
                    varselRow6?.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow6?.varselStatus shouldBe VarselStatus.UKJENT

                    val periodeAvsluttet = periodeAvsluttet(
                        periodeId = aapenPeriode.id,
                        arbeidssoekerId = bekreftelseTilgjengelig1.arbeidssoekerId
                    )
                    bekreftelseHendelseTopic.pipeInput(periodeAvsluttet.asRecord(key))
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                        key shouldBe bekreftelseTilgjengelig1.bekreftelseId.toString()
                        value.eventName shouldBe EventType.Inaktiver
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                        key shouldBe bekreftelseTilgjengelig2.bekreftelseId.toString()
                        value.eventName shouldBe EventType.Inaktiver
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true
                    periodeRepository.findAll() shouldHaveSize 0
                    varselRepository.findAll() shouldHaveSize 0
                }

                "Skal ignorere urelevante hendelser" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    bekreftelsePeriodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    val hendelse1 = baOmAaAvsluttePeriode(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(hendelse1.asRecord(key = key))
                    val hendelse2 = leveringsfristUtloept(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(hendelse2.asRecord(key = key))
                    val hendelse3 = registerGracePeriodeUtloept(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(hendelse3.asRecord(key = key))
                    val hendelse4 = registerGracePeriodeUtloeptEtterEksternInnsamling(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(hendelse4.asRecord(key = key))
                    val hendelse5 = registerGracePeriodeGjenstaaendeTid(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(hendelse5.asRecord(key = key))
                    val hendelse6 = bekreftelsePaaVegneAvStartet(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(hendelse6.asRecord(key = key))
                    val hendelse7 = eksternGracePeriodeUtloept(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(hendelse7.asRecord(key = key))

                    bekreftelseVarselTopic.isEmpty shouldBe true
                    periodeRepository.findAll() shouldHaveSize 0
                    varselRepository.findAll() shouldHaveSize 0
                }

                "Skal feile om periode ikke er mottatt" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    val bekreftelseTilgjengelig1 = bekreftelseTilgjengelig(
                        periodeId = aapenPeriode.id,
                    )
                    val exception = shouldThrow<StreamsException> {
                        bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig1.asRecord(key))
                    }
                    exception.cause should beInstanceOf<PeriodeIkkeFunnetException>()
                }
            }
        }
    }
})


