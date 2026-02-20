package no.nav.paw.arbeidssoekerregisteret.topology

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import no.nav.paw.arbeidssoekerregisteret.context.KafkaTestContext
import no.nav.paw.arbeidssoekerregisteret.exception.PeriodeIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.model.EksterneVarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.PerioderTable
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.model.VarslerTable
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.tms.varsel.action.EksternKanal
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
                "Normal rekkefølge med åpen før lukket periode av sluttbruker" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    periodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    val periodeRows1 = PerioderTable.findAll()
                    periodeRows1 shouldHaveSize 1
                    val periodeRow1 = periodeRows1[0]
                    periodeRow1.periodeId shouldBe aapenPeriode.id
                    periodeRow1.avsluttetTimestamp shouldBe null
                    periodeRow1.updatedTimestamp shouldBe null
                    periodeVarselTopic.isEmpty shouldBe true
                    VarslerTable.findAll() shouldHaveSize 0

                    val lukketPeriode = lukketPeriode(
                        id = aapenPeriode.id,
                        identitetsnummer = aapenPeriode.identitetsnummer,
                        startet = aapenPeriode.startet,
                        avsluttet = metadata(
                            bruker = bruker(
                                type = BrukerType.SLUTTBRUKER,
                                id = aapenPeriode.identitetsnummer
                            )
                        )
                    )
                    periodeTopic.pipeInput(lukketPeriode.asRecord(key))
                    val periodeRows2 = PerioderTable.findAll()
                    periodeRows2 shouldHaveSize 1
                    val periodeRow2 = periodeRows2[0]
                    periodeRow2.periodeId shouldBe aapenPeriode.id
                    periodeRow2.periodeId shouldBe lukketPeriode.id
                    periodeRow2.avsluttetTimestamp shouldNotBe null
                    periodeRow2.updatedTimestamp shouldNotBe null
                    VarslerTable.findAll() shouldHaveSize 1
                    val varselRows1 = VarslerTable.findByPeriodeId(lukketPeriode.id)
                    varselRows1 shouldHaveSize 1
                    val varselRow1 = varselRows1[0]
                    varselRow1 shouldNotBe null
                    varselRow1.periodeId shouldBe lukketPeriode.id
                    varselRow1.bekreftelseId shouldBe null
                    varselRow1.varselKilde shouldBe VarselKilde.PERIODE_AVSLUTTET
                    varselRow1.varselType shouldBe VarselType.BESKJED
                    varselRow1.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow1.varselStatus shouldBe VarselStatus.UKJENT
                    periodeVarselTopic.isEmpty shouldBe false
                    periodeVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe varselRow1.varselId.toString()
                        value.varselId shouldBe varselRow1.varselId.toString()
                        value.type shouldBe Varseltype.Beskjed
                        value.ident shouldBe lukketPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                        value.eksternVarsling shouldBe null
                    }
                    periodeVarselTopic.isEmpty shouldBe true
                    periodeTopic.pipeInput(lukketPeriode.asRecord(key))
                    periodeVarselTopic.isEmpty shouldBe true
                }

                "Normal rekkefølge med åpen før lukket periode av system" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    periodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    val periodeRows1 = PerioderTable.findAll()
                    periodeRows1 shouldHaveSize 1
                    val periodeRow1 = periodeRows1[0]
                    periodeRow1.periodeId shouldBe aapenPeriode.id
                    periodeRow1.avsluttetTimestamp shouldBe null
                    periodeRow1.updatedTimestamp shouldBe null
                    periodeVarselTopic.isEmpty shouldBe true
                    VarslerTable.findAll() shouldHaveSize 0

                    val lukketPeriode = lukketPeriode(
                        id = aapenPeriode.id,
                        identitetsnummer = aapenPeriode.identitetsnummer,
                        startet = aapenPeriode.startet
                    )
                    periodeTopic.pipeInput(lukketPeriode.asRecord(key))
                    val periodeRows2 = PerioderTable.findAll()
                    periodeRows2 shouldHaveSize 1
                    val periodeRow2 = periodeRows2[0]
                    periodeRow2.periodeId shouldBe aapenPeriode.id
                    periodeRow2.periodeId shouldBe lukketPeriode.id
                    periodeRow2.avsluttetTimestamp shouldNotBe null
                    periodeRow2.updatedTimestamp shouldNotBe null
                    VarslerTable.findAll() shouldHaveSize 1
                    val varselRows1 = VarslerTable.findByPeriodeId(lukketPeriode.id)
                    varselRows1 shouldHaveSize 1
                    val varselRow1 = varselRows1[0]
                    varselRow1 shouldNotBe null
                    varselRow1.periodeId shouldBe lukketPeriode.id
                    varselRow1.bekreftelseId shouldBe null
                    varselRow1.varselKilde shouldBe VarselKilde.PERIODE_AVSLUTTET
                    varselRow1.varselType shouldBe VarselType.BESKJED
                    varselRow1.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow1.varselStatus shouldBe VarselStatus.UKJENT
                    periodeVarselTopic.isEmpty shouldBe false
                    periodeVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe varselRow1.varselId.toString()
                        value.varselId shouldBe varselRow1.varselId.toString()
                        value.type shouldBe Varseltype.Beskjed
                        value.ident shouldBe lukketPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                        value.eksternVarsling shouldNotBe null
                        value.eksternVarsling!!.prefererteKanaler shouldContain EksternKanal.BETINGET_SMS
                    }
                    periodeVarselTopic.isEmpty shouldBe true
                    periodeTopic.pipeInput(lukketPeriode.asRecord(key))
                    periodeVarselTopic.isEmpty shouldBe true
                }

                "Motsatt rekkefølge med lukket før åpen periode av system" {
                    val key = Random.nextLong()
                    val lukketPeriode = lukketPeriode()
                    periodeTopic.pipeInput(lukketPeriode.asRecord(key))
                    val periodeRows1 = PerioderTable.findAll()
                    periodeRows1 shouldHaveSize 1
                    val periodeRow1 = periodeRows1[0]
                    periodeRow1.periodeId shouldBe lukketPeriode.id
                    periodeRow1.avsluttetTimestamp shouldNotBe null
                    periodeRow1.updatedTimestamp shouldBe null
                    VarslerTable.findAll() shouldHaveSize 1
                    val varselRows1 = VarslerTable.findByPeriodeId(lukketPeriode.id)
                    val varselRow1 = varselRows1[0]
                    varselRow1 shouldNotBe null
                    varselRow1.periodeId shouldBe lukketPeriode.id
                    varselRow1.bekreftelseId shouldBe null
                    varselRow1.varselKilde shouldBe VarselKilde.PERIODE_AVSLUTTET
                    varselRow1.varselType shouldBe VarselType.BESKJED
                    varselRow1.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow1.varselStatus shouldBe VarselStatus.UKJENT
                    periodeVarselTopic.isEmpty shouldBe false
                    periodeVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe varselRow1.varselId.toString()
                        value.varselId shouldBe varselRow1.varselId.toString()
                        value.type shouldBe Varseltype.Beskjed
                        value.ident shouldBe lukketPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                        value.eksternVarsling shouldNotBe null
                        value.eksternVarsling!!.prefererteKanaler shouldContain EksternKanal.BETINGET_SMS
                    }
                    periodeVarselTopic.isEmpty shouldBe true

                    val aapenPeriode = aapenPeriode(
                        id = lukketPeriode.id,
                        identitetsnummer = lukketPeriode.identitetsnummer,
                        startet = lukketPeriode.startet
                    )
                    periodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    periodeVarselTopic.isEmpty shouldBe true
                    val periodeRows2 = PerioderTable.findAll()
                    periodeRows2 shouldHaveSize 1
                    val periodeRow2 = periodeRows2[0]
                    periodeRow2.periodeId shouldBe aapenPeriode.id
                    periodeRow2.periodeId shouldBe lukketPeriode.id
                    periodeRow2.avsluttetTimestamp shouldNotBe null
                    periodeRow2.updatedTimestamp shouldBe null
                }
            }

            "Verifiser håndtering av bekreftelser" - {
                "Bekreftelse tilgjengelig så bekreftelse mottatt" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    bekreftelsePeriodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    val bekreftelseTilgjengelig = bekreftelseTilgjengelig(
                        periodeId = aapenPeriode.id,
                        hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(10))
                    )
                    bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig.asRecord(key))
                    VarslerTable.findAll() shouldHaveSize 1
                    val varselRow1 = VarslerTable.findByBekreftelseId(bekreftelseTilgjengelig.bekreftelseId)
                    varselRow1 shouldNotBe null
                    varselRow1!!.periodeId shouldBe bekreftelseTilgjengelig.periodeId
                    varselRow1.bekreftelseId shouldBe bekreftelseTilgjengelig.bekreftelseId
                    varselRow1.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow1.varselType shouldBe VarselType.OPPGAVE
                    varselRow1.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow1.varselStatus shouldBe VarselStatus.UKJENT
                    varselRow1.eksterntVarsel shouldBe null
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe varselRow1.varselId.toString()
                        value.varselId shouldBe varselRow1.varselId.toString()
                        value.type shouldBe Varseltype.Oppgave
                        value.ident shouldBe aapenPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                        value.eksternVarsling shouldNotBe null
                        value.eksternVarsling!!.prefererteKanaler shouldContain EksternKanal.BETINGET_SMS
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true

                    val oppgaveVarselHendelse1 = varselHendelse(
                        eventName = VarselEventName.OPPRETTET,
                        varselId = varselRow1.varselId.toString(),
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(5))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse1)
                    val oppgaveVarselHendelse2 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = varselRow1.varselId.toString(),
                        status = VarselStatus.BESTILT,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(4))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse2)
                    val oppgaveVarselHendelse3 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = varselRow1.varselId.toString(),
                        status = VarselStatus.SENDT,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(3))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse3)
                    val oppgaveVarselHendelse4 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = varselRow1.varselId.toString(),
                        status = VarselStatus.FERDIGSTILT,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(2))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse4)
                    val oppgaveVarselHendelse5 = varselHendelse(
                        eventName = VarselEventName.INAKTIVERT,
                        varselId = varselRow1.varselId.toString(),
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(1))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse5)
                    VarslerTable.findAll() shouldHaveSize 1
                    val varselRow2 = VarslerTable.findByBekreftelseId(bekreftelseTilgjengelig.bekreftelseId)
                    varselRow2 shouldNotBe null
                    varselRow2!!.periodeId shouldBe bekreftelseTilgjengelig.periodeId
                    varselRow2.bekreftelseId shouldBe bekreftelseTilgjengelig.bekreftelseId
                    varselRow2.varselId shouldBe varselRow1.varselId
                    varselRow2.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow2.varselType shouldBe VarselType.OPPGAVE
                    varselRow2.hendelseName shouldBe VarselEventName.INAKTIVERT
                    varselRow2.varselStatus shouldBe VarselStatus.UKJENT
                    varselRow2.eksterntVarsel shouldNotBe null
                    varselRow2.eksterntVarsel?.varselType shouldBe VarselType.OPPGAVE
                    varselRow2.eksterntVarsel?.hendelseName shouldBe VarselEventName.EKSTERN_STATUS_OPPDATERT
                    varselRow2.eksterntVarsel?.varselStatus shouldBe VarselStatus.FERDIGSTILT

                    val bekreftelseMeldingMottatt = bekreftelseMeldingMottatt(
                        periodeId = aapenPeriode.id,
                        bekreftelseId = bekreftelseTilgjengelig.bekreftelseId,
                        arbeidssoekerId = bekreftelseTilgjengelig.arbeidssoekerId,
                    )
                    bekreftelseHendelseTopic.pipeInput(bekreftelseMeldingMottatt.asRecord(key))
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                        key shouldBe varselRow2.varselId.toString()
                        value.varselId shouldBe varselRow2.varselId.toString()
                        value.eventName shouldBe EventType.Inaktiver
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true
                    VarslerTable.findAll() shouldHaveSize 1
                    EksterneVarslerTable.findAll() shouldHaveSize 1
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
                    VarslerTable.findAll() shouldHaveSize 1
                    val varselRow1 = VarslerTable.findByBekreftelseId(bekreftelseTilgjengelig1.bekreftelseId)
                    varselRow1 shouldNotBe null
                    varselRow1!!.periodeId shouldBe bekreftelseTilgjengelig1.periodeId
                    varselRow1.bekreftelseId shouldBe bekreftelseTilgjengelig1.bekreftelseId
                    varselRow1.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow1.varselType shouldBe VarselType.OPPGAVE
                    varselRow1.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow1.varselStatus shouldBe VarselStatus.UKJENT
                    varselRow1.eksterntVarsel shouldBe null
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe varselRow1.varselId.toString()
                        value.varselId shouldBe varselRow1.varselId.toString()
                        value.type shouldBe Varseltype.Oppgave
                        value.ident shouldBe aapenPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                        value.eksternVarsling shouldNotBe null
                        value.eksternVarsling!!.prefererteKanaler shouldContain EksternKanal.BETINGET_SMS
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true

                    val oppgaveVarselHendelse1 = varselHendelse(
                        eventName = VarselEventName.OPPRETTET,
                        varselId = varselRow1.varselId.toString(),
                        status = VarselStatus.VENTER,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(3))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse1)
                    val oppgaveVarselHendelse2 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = varselRow1.varselId.toString(),
                        status = VarselStatus.BESTILT,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(2))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse2)
                    val oppgaveVarselHendelse3 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = varselRow1.varselId.toString(),
                        status = VarselStatus.FEILET,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(1))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse3)
                    val oppgaveVarselHendelse4 = varselHendelse(
                        eventName = VarselEventName.INAKTIVERT,
                        varselId = varselRow1.varselId.toString(),
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(10))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse4)
                    val varselRow2 = VarslerTable.findByBekreftelseId(bekreftelseTilgjengelig1.bekreftelseId)
                    varselRow2 shouldNotBe null
                    varselRow2!!.periodeId shouldBe bekreftelseTilgjengelig1.periodeId
                    varselRow2.bekreftelseId shouldBe bekreftelseTilgjengelig1.bekreftelseId
                    varselRow2.varselId shouldBe varselRow1.varselId
                    varselRow2.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow2.varselType shouldBe VarselType.OPPGAVE
                    varselRow2.hendelseName shouldBe VarselEventName.OPPRETTET
                    varselRow2.varselStatus shouldBe VarselStatus.VENTER
                    varselRow2.eksterntVarsel shouldNotBe null
                    varselRow2.eksterntVarsel?.varselType shouldBe VarselType.OPPGAVE
                    varselRow2.eksterntVarsel?.hendelseName shouldBe VarselEventName.EKSTERN_STATUS_OPPDATERT
                    varselRow2.eksterntVarsel?.varselStatus shouldBe VarselStatus.FEILET

                    val bekreftelseTilgjengelig2 = bekreftelseTilgjengelig(
                        periodeId = aapenPeriode.id,
                        hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(10))
                    )
                    bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig2.asRecord(key))
                    VarslerTable.findAll() shouldHaveSize 2
                    val varselRow3 = VarslerTable.findByBekreftelseId(bekreftelseTilgjengelig2.bekreftelseId)
                    varselRow3 shouldNotBe null
                    varselRow3!!.periodeId shouldBe bekreftelseTilgjengelig2.periodeId
                    varselRow3.bekreftelseId shouldBe bekreftelseTilgjengelig2.bekreftelseId
                    varselRow3.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow3.varselType shouldBe VarselType.OPPGAVE
                    varselRow3.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow3.varselStatus shouldBe VarselStatus.UKJENT
                    varselRow3.eksterntVarsel shouldBe null
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe varselRow3.varselId.toString()
                        value.varselId shouldBe varselRow3.varselId.toString()
                        value.type shouldBe Varseltype.Oppgave
                        value.ident shouldBe aapenPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                        value.eksternVarsling shouldNotBe null
                        value.eksternVarsling!!.prefererteKanaler shouldContain EksternKanal.BETINGET_SMS
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true

                    val periodeAvsluttet = periodeAvsluttet(
                        periodeId = aapenPeriode.id,
                        arbeidssoekerId = bekreftelseTilgjengelig1.arbeidssoekerId
                    )
                    bekreftelseHendelseTopic.pipeInput(periodeAvsluttet.asRecord(key))
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                        key shouldBe varselRow1.varselId.toString()
                        value.varselId shouldBe varselRow1.varselId.toString()
                        value.eventName shouldBe EventType.Inaktiver
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                        key shouldBe varselRow3.varselId.toString()
                        value.varselId shouldBe varselRow3.varselId.toString()
                        value.eventName shouldBe EventType.Inaktiver
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true
                    PerioderTable.findAll() shouldHaveSize 0
                    VarslerTable.findAll() shouldHaveSize 2
                    EksterneVarslerTable.findAll() shouldHaveSize 1
                }

                "To bekreftelser tilgjengelig så på vegne av startet" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    bekreftelsePeriodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    val bekreftelseTilgjengelig1 = bekreftelseTilgjengelig(
                        periodeId = aapenPeriode.id,
                        hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(20))
                    )
                    bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig1.asRecord(key))
                    VarslerTable.findAll() shouldHaveSize 1
                    val varselRow1 = VarslerTable.findByBekreftelseId(bekreftelseTilgjengelig1.bekreftelseId)
                    varselRow1 shouldNotBe null
                    varselRow1!!.periodeId shouldBe bekreftelseTilgjengelig1.periodeId
                    varselRow1.bekreftelseId shouldBe bekreftelseTilgjengelig1.bekreftelseId
                    varselRow1.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow1.varselType shouldBe VarselType.OPPGAVE
                    varselRow1.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow1.varselStatus shouldBe VarselStatus.UKJENT
                    varselRow1.eksterntVarsel shouldBe null
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe varselRow1.varselId.toString()
                        value.varselId shouldBe varselRow1.varselId.toString()
                        value.type shouldBe Varseltype.Oppgave
                        value.ident shouldBe aapenPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                        value.eksternVarsling shouldNotBe null
                        value.eksternVarsling!!.prefererteKanaler shouldContain EksternKanal.BETINGET_SMS
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true

                    val oppgaveVarselHendelse1 = varselHendelse(
                        eventName = VarselEventName.OPPRETTET,
                        varselId = varselRow1.varselId.toString(),
                        status = VarselStatus.VENTER,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(3))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse1)
                    val oppgaveVarselHendelse2 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = varselRow1.varselId.toString(),
                        status = VarselStatus.BESTILT,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(2))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse2)
                    val oppgaveVarselHendelse3 = varselHendelse(
                        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
                        varselId = varselRow1.varselId.toString(),
                        status = VarselStatus.FEILET,
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(1))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse3)
                    val oppgaveVarselHendelse4 = varselHendelse(
                        eventName = VarselEventName.INAKTIVERT,
                        varselId = varselRow1.varselId.toString(),
                        varseltype = VarselType.OPPGAVE,
                        tidspunkt = Instant.now().minus(Duration.ofMinutes(10))
                    )
                    varselHendelseTopic.pipeInput(oppgaveVarselHendelse4)
                    val varselRow2 = VarslerTable.findByBekreftelseId(bekreftelseTilgjengelig1.bekreftelseId)
                    varselRow2 shouldNotBe null
                    varselRow2!!.periodeId shouldBe bekreftelseTilgjengelig1.periodeId
                    varselRow2.bekreftelseId shouldBe bekreftelseTilgjengelig1.bekreftelseId
                    varselRow2.varselId shouldBe varselRow1.varselId
                    varselRow2.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow2.varselType shouldBe VarselType.OPPGAVE
                    varselRow2.hendelseName shouldBe VarselEventName.OPPRETTET
                    varselRow2.varselStatus shouldBe VarselStatus.VENTER
                    varselRow2.eksterntVarsel shouldNotBe null
                    varselRow2.eksterntVarsel?.varselType shouldBe VarselType.OPPGAVE
                    varselRow2.eksterntVarsel?.hendelseName shouldBe VarselEventName.EKSTERN_STATUS_OPPDATERT
                    varselRow2.eksterntVarsel?.varselStatus shouldBe VarselStatus.FEILET

                    val bekreftelseTilgjengelig2 = bekreftelseTilgjengelig(
                        periodeId = aapenPeriode.id,
                        hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(10))
                    )
                    bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig2.asRecord(key))
                    VarslerTable.findAll() shouldHaveSize 2
                    val varselRow3 = VarslerTable.findByBekreftelseId(bekreftelseTilgjengelig2.bekreftelseId)
                    varselRow3 shouldNotBe null
                    varselRow3!!.periodeId shouldBe bekreftelseTilgjengelig2.periodeId
                    varselRow3.bekreftelseId shouldBe bekreftelseTilgjengelig2.bekreftelseId
                    varselRow3.varselId shouldNotBe varselRow2.varselId
                    varselRow3.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow3.varselType shouldBe VarselType.OPPGAVE
                    varselRow3.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow3.varselStatus shouldBe VarselStatus.UKJENT
                    varselRow3.eksterntVarsel shouldBe null
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<OpprettVarsel>(stringValue)
                        key shouldBe varselRow3.varselId.toString()
                        value.varselId shouldBe varselRow3.varselId.toString()
                        value.type shouldBe Varseltype.Oppgave
                        value.ident shouldBe aapenPeriode.identitetsnummer
                        value.eventName shouldBe EventType.Opprett
                        value.eksternVarsling shouldNotBe null
                        value.eksternVarsling!!.prefererteKanaler shouldContain EksternKanal.BETINGET_SMS
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true

                    val paaVegneAvStartet = bekreftelsePaaVegneAvStartet(
                        periodeId = aapenPeriode.id,
                        arbeidssoekerId = bekreftelseTilgjengelig1.arbeidssoekerId
                    )
                    bekreftelseHendelseTopic.pipeInput(paaVegneAvStartet.asRecord(key))
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                        key shouldBe varselRow1.varselId.toString()
                        value.varselId shouldBe varselRow1.varselId.toString()
                        value.eventName shouldBe EventType.Inaktiver
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                        key shouldBe varselRow3.varselId.toString()
                        value.varselId shouldBe varselRow3.varselId.toString()
                        value.eventName shouldBe EventType.Inaktiver
                    }
                    bekreftelseVarselTopic.isEmpty shouldBe true
                    PerioderTable.findAll() shouldHaveSize 0
                    VarslerTable.findAll() shouldHaveSize 2
                    EksterneVarslerTable.findAll() shouldHaveSize 1
                }

                "Skal ignorere urelevante hendelser" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    bekreftelsePeriodeTopic.pipeInput(aapenPeriode.asRecord(key))
                    val bekreftelseTilgjengelig = bekreftelseTilgjengelig(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig.asRecord(key))
                    val hendelse1 = baOmAaAvsluttePeriode(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(hendelse1.asRecord(key = key))
                    val hendelse2 = leveringsfristUtloept(
                        periodeId = aapenPeriode.id,
                        bekreftelseId = bekreftelseTilgjengelig.bekreftelseId
                    )
                    bekreftelseHendelseTopic.pipeInput(hendelse2.asRecord(key = key))
                    val hendelse3 = registerGracePeriodeUtloept(
                        periodeId = aapenPeriode.id,
                        bekreftelseId = bekreftelseTilgjengelig.bekreftelseId
                    )
                    bekreftelseHendelseTopic.pipeInput(hendelse3.asRecord(key = key))
                    val hendelse4 = registerGracePeriodeUtloeptEtterEksternInnsamling(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(hendelse4.asRecord(key = key))
                    val hendelse5 = registerGracePeriodeGjenstaaendeTid(
                        periodeId = aapenPeriode.id,
                        bekreftelseId = bekreftelseTilgjengelig.bekreftelseId
                    )
                    bekreftelseHendelseTopic.pipeInput(hendelse5.asRecord(key = key))
                    val hendelse6 = eksternGracePeriodeUtloept(periodeId = aapenPeriode.id)
                    bekreftelseHendelseTopic.pipeInput(hendelse6.asRecord(key = key))

                    PerioderTable.findAll() shouldHaveSize 0
                    EksterneVarslerTable.findAll() shouldHaveSize 0
                    VarslerTable.findAll() shouldHaveSize 1
                    val varselRow = VarslerTable.findByBekreftelseId(bekreftelseTilgjengelig.bekreftelseId)
                    varselRow shouldNotBe null
                    varselRow!!.periodeId shouldBe bekreftelseTilgjengelig.periodeId
                    varselRow.bekreftelseId shouldBe bekreftelseTilgjengelig.bekreftelseId
                    varselRow.varselKilde shouldBe VarselKilde.BEKREFTELSE_TILGJENGELIG
                    varselRow.varselType shouldBe VarselType.OPPGAVE
                    varselRow.hendelseName shouldBe VarselEventName.UKJENT
                    varselRow.varselStatus shouldBe VarselStatus.UKJENT
                    varselRow.eksterntVarsel shouldBe null
                    bekreftelseVarselTopic.isEmpty shouldBe false
                    bekreftelseVarselTopic.readKeyValue() should { (key, stringValue) ->
                        val value = objectMapper.readValue<InaktiverVarsel>(stringValue)
                        key shouldBe varselRow.varselId.toString()
                        value.varselId shouldBe varselRow.varselId.toString()
                        value.eventName shouldBe EventType.Opprett
                    }
                }

                "Skal feile om periode ikke er mottatt" {
                    val key = Random.nextLong()
                    val aapenPeriode = aapenPeriode()
                    val bekreftelseTilgjengelig = bekreftelseTilgjengelig(periodeId = aapenPeriode.id)
                    val exception = shouldThrow<StreamsException> {
                        bekreftelseHendelseTopic.pipeInput(bekreftelseTilgjengelig.asRecord(key))
                    }
                    exception.cause should beInstanceOf<PeriodeIkkeFunnetException>()
                }
            }
        }
    }
})


