package no.nav.paw.arbeidssoekerregisteret.test

import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselKanal
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.EksternGracePeriodeUtloept
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloeptEtterEksternInnsamling
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.env.namespaceOrDefaultForLocal
import org.apache.kafka.streams.test.TestRecord
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.random.Random

object TestData {
    val runtimeEnvironment = currentRuntimeEnvironment

    val identitetsnummer1 = "01017012345"
    val identitetsnummer2 = "02017012345"
    val identitetsnummer3 = "03017012345"
    val identitetsnummer4 = "04017012345"
    val identitetsnummer5 = "05017012345"
    val arbeidssoekerId1 = 10001L
    val arbeidssoekerId2 = 10002L
    val arbeidssoekerId3 = 10003L
    val arbeidssoekerId4 = 10004L
    val arbeidssoekerId5 = 10005L
    val kafkaKey1 = -10001L
    val kafkaKey2 = -10002L
    val kafkaKey3 = -10003L
    val kafkaKey4 = -10004L
    val kafkaKey5 = -10005L
    val periodeId1 = UUID.fromString("4c0cb50a-3b4a-45df-b5b6-2cb45f04d19b")
    val periodeId2 = UUID.fromString("0fc3de47-a6cd-4ad5-8433-53235738200d")
    val periodeId3 = UUID.fromString("12cf8147-a76d-4b62-85d2-4792fea08995")
    val periodeId4 = UUID.fromString("f6f2f98a-2f2b-401d-b837-6ad26e45d4bf")
    val periodeId5 = UUID.fromString("f6384bc5-a0ec-4bdc-9262-f6ebf952269f")
    val bekreftelseId1 = UUID.fromString("0cd73e66-e5a2-4dae-88de-2dd89a910a19")
    val bekreftelseId2 = UUID.fromString("7b769364-4d48-40f8-ac64-4489bb8080dd")
    val bekreftelseId3 = UUID.fromString("b6e3b543-da44-4524-860f-9474bd6d505e")
    val bekreftelseId4 = UUID.fromString("a59581e6-c9be-4aec-b9f4-c635f1826c71")
    val bekreftelseId5 = UUID.fromString("de94b7ab-360f-4e5f-9ad1-3dc572b3e6a5")

    fun Periode.asRecord(key: Long = longKey()): TestRecord<Long, Periode> =
        TestRecord(key, this)

    fun BekreftelseTilgjengelig.asRecord(key: Long = longKey()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun BekreftelseMeldingMottatt.asRecord(key: Long = longKey()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun PeriodeAvsluttet.asRecord(key: Long = longKey()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun BaOmAaAvsluttePeriode.asRecord(key: Long = longKey()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun LeveringsfristUtloept.asRecord(key: Long = longKey()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun RegisterGracePeriodeUtloept.asRecord(key: Long = longKey()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun RegisterGracePeriodeUtloeptEtterEksternInnsamling.asRecord(key: Long = longKey()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun RegisterGracePeriodeGjenstaaendeTid.asRecord(key: Long = longKey()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun BekreftelsePaaVegneAvStartet.asRecord(key: Long = longKey()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun EksternGracePeriodeUtloept.asRecord(key: Long = longKey()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun VarselHendelse.asRecord(): TestRecord<String, VarselHendelse> =
        TestRecord(this.varselId, this)

    val aapenPeriode1 = aapenPeriode(
        id = periodeId1,
        identitetsnummer = identitetsnummer1,
        startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(1))),
    )
    val aapenPeriode2 = aapenPeriode(
        id = periodeId2,
        identitetsnummer = identitetsnummer2,
        startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(2)))
    )
    val aapenPeriode3 = aapenPeriode(
        id = periodeId3,
        identitetsnummer = identitetsnummer3,
        startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(2)))
    )

    val lukketPeriode1 = lukketPeriode(
        id = periodeId1,
        identitetsnummer = identitetsnummer1,
        startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(1))),
        avsluttet = metadata(tidspunkt = Instant.now())
    )
    val lukketPeriode2 = lukketPeriode(
        id = periodeId2,
        identitetsnummer = identitetsnummer2,
        startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(2))),
        avsluttet = metadata(tidspunkt = Instant.now())
    )
    val lukketPeriode3 = lukketPeriode(
        id = periodeId3,
        identitetsnummer = identitetsnummer3,
        startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(2))),
        avsluttet = metadata(tidspunkt = Instant.now())
    )

    val bekreftelseTilgjengelig1a = bekreftelseTilgjengelig(
        periodeId = periodeId1,
        bekreftelseId = bekreftelseId1,
        arbeidssoekerId = arbeidssoekerId1,
        hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(50))
    )
    val bekreftelseTilgjengelig1b = bekreftelseTilgjengelig(
        periodeId = periodeId1,
        bekreftelseId = bekreftelseId2,
        arbeidssoekerId = arbeidssoekerId1,
        hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(40))
    )
    val bekreftelseTilgjengelig1c = bekreftelseTilgjengelig(
        periodeId = periodeId1,
        bekreftelseId = bekreftelseId3,
        arbeidssoekerId = arbeidssoekerId1,
        hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(30))
    )
    val bekreftelseMeldingMottatt1 = bekreftelseMeldingMottatt(
        periodeId = periodeId1,
        bekreftelseId = bekreftelseId1,
        arbeidssoekerId = arbeidssoekerId1,
    )
    val baOmAaAvsluttePeriode1 = baOmAaAvsluttePeriode(
        periodeId = periodeId1,
        arbeidssoekerId = arbeidssoekerId1
    )
    val bekreftelsePaaVegneAvStartet1 = bekreftelsePaaVegneAvStartet(
        periodeId = periodeId1,
        arbeidssoekerId = arbeidssoekerId1
    )
    val registerGracePeriodeUtloept1 = registerGracePeriodeUtloept(
        periodeId = periodeId1,
        arbeidssoekerId = arbeidssoekerId1,
        bekreftelseId = bekreftelseId1
    )
    val registerGracePeriodeUtloeptEtterEksternInnsamling1 = registerGracePeriodeUtloeptEtterEksternInnsamling(
        periodeId = periodeId1,
        arbeidssoekerId = arbeidssoekerId1
    )
    val registerGracePeriodeGjenstaaendeTid1 = registerGracePeriodeGjenstaaendeTid(
        periodeId = periodeId1,
        arbeidssoekerId = arbeidssoekerId1,
        bekreftelseId = bekreftelseId1
    )
    val eksternGracePeriodeUtloept1 = eksternGracePeriodeUtloept(
        periodeId = periodeId1,
        arbeidssoekerId = arbeidssoekerId1
    )
    val leveringsfristUtloept1 = leveringsfristUtloept(
        periodeId = periodeId1,
        arbeidssoekerId = arbeidssoekerId1
    )
    val periodeAvsluttet1 = periodeAvsluttet(
        periodeId = periodeId1,
        arbeidssoekerId = arbeidssoekerId1
    )

    val oppgaveVarselHendelse1a = varselHendelse(
        eventName = VarselEventName.OPPRETTET,
        varselId = bekreftelseId1.toString(),
        varseltype = VarselType.OPPGAVE,
        tidspunkt = Instant.now().minus(Duration.ofMinutes(5))
    )
    val oppgaveVarselHendelse1b = varselHendelse(
        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
        varselId = bekreftelseId1.toString(),
        status = VarselStatus.BESTILT,
        varseltype = VarselType.OPPGAVE,
        tidspunkt = Instant.now().minus(Duration.ofMinutes(4))
    )
    val oppgaveVarselHendelse1c = varselHendelse(
        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
        varselId = bekreftelseId1.toString(),
        status = VarselStatus.SENDT,
        varseltype = VarselType.OPPGAVE,
        tidspunkt = Instant.now().minus(Duration.ofMinutes(3))
    )
    val oppgaveVarselHendelse1d = varselHendelse(
        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
        varselId = bekreftelseId1.toString(),
        status = VarselStatus.FERDIGSTILT,
        varseltype = VarselType.OPPGAVE,
        tidspunkt = Instant.now().minus(Duration.ofMinutes(2))
    )
    val oppgaveVarselHendelse1e = varselHendelse(
        eventName = VarselEventName.INAKTIVERT,
        varselId = bekreftelseId1.toString(),
        varseltype = VarselType.OPPGAVE,
        tidspunkt = Instant.now().minus(Duration.ofMinutes(1))
    )
    val oppgaveVarselHendelse2a = varselHendelse(
        eventName = VarselEventName.OPPRETTET,
        varselId = bekreftelseId2.toString(),
        status = VarselStatus.VENTER,
        varseltype = VarselType.OPPGAVE,
        tidspunkt = Instant.now().minus(Duration.ofMinutes(3))
    )
    val oppgaveVarselHendelse2b = varselHendelse(
        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
        varselId = bekreftelseId2.toString(),
        status = VarselStatus.BESTILT,
        varseltype = VarselType.OPPGAVE,
        tidspunkt = Instant.now().minus(Duration.ofMinutes(2))
    )
    val oppgaveVarselHendelse2c = varselHendelse(
        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
        varselId = bekreftelseId2.toString(),
        status = VarselStatus.FEILET,
        varseltype = VarselType.OPPGAVE,
        tidspunkt = Instant.now().minus(Duration.ofMinutes(1))
    )
    val oppgaveVarselHendelse2d = varselHendelse(
        eventName = VarselEventName.INAKTIVERT,
        varselId = bekreftelseId2.toString(),
        varseltype = VarselType.OPPGAVE,
        tidspunkt = Instant.now().minus(Duration.ofMinutes(10))
    )

    fun bruker(
        type: BrukerType = BrukerType.SYSTEM,
        id: String = "test"
    ): Bruker = Bruker(type, id)

    fun metadata(
        tidspunkt: Instant = Instant.now(),
        bruker: Bruker = bruker(),
    ): Metadata = Metadata(
        tidspunkt,
        bruker,
        "test",
        "test",
        null
    )

    fun aapenPeriode(
        id: UUID = periodeId1,
        identitetsnummer: String = identitetsnummer1,
        startet: Metadata = metadata()
    ): Periode = Periode(id, identitetsnummer, startet, null)

    fun lukketPeriode(
        id: UUID = periodeId1,
        identitetsnummer: String = identitetsnummer1,
        startet: Metadata = metadata(tidspunkt = Instant.now().minus(Duration.ofDays(30))),
        avsluttet: Metadata = metadata(),
    ): Periode = Periode(id, identitetsnummer, startet, avsluttet)

    fun bekreftelseTilgjengelig(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        bekreftelseId: UUID = bekreftelseId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        gjelderFra: Instant = Instant.now(),
        gjelderTil: Instant = Instant.now().plus(Duration.ofDays(14)),
        hendelseTidspunkt: Instant = Instant.now()
    ): BekreftelseTilgjengelig = BekreftelseTilgjengelig(
        hendelseId = hendelseId,
        periodeId = periodeId,
        bekreftelseId = bekreftelseId,
        arbeidssoekerId = arbeidssoekerId,
        gjelderFra = gjelderFra,
        gjelderTil = gjelderTil,
        hendelseTidspunkt = hendelseTidspunkt
    )

    fun bekreftelseMeldingMottatt(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        hendelseTidspunkt: Instant = Instant.now(),
        bekreftelseId: UUID = bekreftelseId1,
    ): BekreftelseMeldingMottatt = BekreftelseMeldingMottatt(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        bekreftelseId = bekreftelseId,
    )

    fun baOmAaAvsluttePeriode(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        hendelseTidspunkt: Instant = Instant.now()
    ): BaOmAaAvsluttePeriode = BaOmAaAvsluttePeriode(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt
    )

    fun bekreftelsePaaVegneAvStartet(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        hendelseTidspunkt: Instant = Instant.now()
    ): BekreftelsePaaVegneAvStartet = BekreftelsePaaVegneAvStartet(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt
    )

    fun leveringsfristUtloept(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        hendelseTidspunkt: Instant = Instant.now(),
        bekreftelseId: UUID = bekreftelseId1,
        leveringsfrist: Instant = Instant.now().minus(Duration.ofHours(1))
    ): LeveringsfristUtloept = LeveringsfristUtloept(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        bekreftelseId = bekreftelseId,
        leveringsfrist = leveringsfrist
    )

    fun registerGracePeriodeUtloept(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        hendelseTidspunkt: Instant = Instant.now(),
        bekreftelseId: UUID = bekreftelseId1
    ): RegisterGracePeriodeUtloept = RegisterGracePeriodeUtloept(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        bekreftelseId = bekreftelseId
    )

    fun registerGracePeriodeUtloeptEtterEksternInnsamling(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        hendelseTidspunkt: Instant = Instant.now()
    ): RegisterGracePeriodeUtloeptEtterEksternInnsamling = RegisterGracePeriodeUtloeptEtterEksternInnsamling(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt
    )

    fun registerGracePeriodeGjenstaaendeTid(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        hendelseTidspunkt: Instant = Instant.now(),
        bekreftelseId: UUID = bekreftelseId1,
        gjenstaandeTid: Duration = Duration.ofDays(1),
    ): RegisterGracePeriodeGjenstaaendeTid = RegisterGracePeriodeGjenstaaendeTid(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        bekreftelseId = bekreftelseId,
        gjenstaandeTid = gjenstaandeTid
    )

    fun eksternGracePeriodeUtloept(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        hendelseTidspunkt: Instant = Instant.now(),
        paaVegneAvNamespace: String = runtimeEnvironment.namespaceOrDefaultForLocal(),
        paaVegneAvId: String = runtimeEnvironment.appNameOrDefaultForLocal()
    ): EksternGracePeriodeUtloept = EksternGracePeriodeUtloept(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        paaVegneAvNamespace = paaVegneAvNamespace,
        paaVegneAvId = paaVegneAvId
    )

    fun periodeAvsluttet(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        hendelseTidspunkt: Instant = Instant.now()
    ): PeriodeAvsluttet = PeriodeAvsluttet(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt
    )

    fun varselHendelse(
        eventName: VarselEventName = VarselEventName.UKJENT,
        status: VarselStatus? = null,
        varselId: String = bekreftelseId1.toString(),
        varseltype: VarselType = VarselType.UKJENT,
        kanal: VarselKanal? = VarselKanal.SMS,
        renotifikasjon: Boolean? = null,
        sendtSomBatch: Boolean? = null,
        feilmelding: String? = null,
        namespace: String = runtimeEnvironment.namespaceOrDefaultForLocal(),
        appnavn: String = runtimeEnvironment.appNameOrDefaultForLocal(),
        tidspunkt: Instant = Instant.now()
    ): VarselHendelse = VarselHendelse(
        eventName = eventName,
        status = status,
        varselId = varselId,
        varseltype = varseltype,
        kanal = kanal,
        renotifikasjon = renotifikasjon,
        sendtSomBatch = sendtSomBatch,
        feilmelding = feilmelding,
        namespace = namespace,
        appnavn = appnavn,
        tidspunkt = tidspunkt
    )

    fun longKey(): Long = Random.nextLong()
}