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
import no.nav.paw.bekreftelse.internehendelser.*
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.env.namespaceOrDefaultForLocal
import org.apache.kafka.streams.test.TestRecord
import java.time.Duration
import java.time.Instant
import java.util.*

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

    val aapenPeriode1 = TestRecord(
        kafkaKey1, aapenPeriode(
            id = periodeId1,
            identitetsnummer = identitetsnummer1,
            startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(1))),
        )
    )
    val aapenPeriode2 = TestRecord(
        kafkaKey2, aapenPeriode(
            id = periodeId2,
            identitetsnummer = identitetsnummer2,
            startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(2)))
        )
    )
    val aapenPeriode3 = TestRecord(
        kafkaKey3, aapenPeriode(
            id = periodeId3,
            identitetsnummer = identitetsnummer3,
            startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(3)))
        )
    )
    val aapenPeriode4 = TestRecord(
        kafkaKey4, aapenPeriode(
            id = periodeId4, identitetsnummer = identitetsnummer4,
            startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(4)))
        )
    )
    val aapenPeriode5 = TestRecord(
        kafkaKey5, aapenPeriode(
            id = periodeId5, identitetsnummer = identitetsnummer5,
            startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(5)))
        )
    )

    val lukketPeriode1 = TestRecord(
        kafkaKey1, lukketPeriode(
            id = periodeId1,
            identitetsnummer = identitetsnummer1,
            startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(1))),
            avsluttet = metadata(tidspunkt = Instant.now())
        )
    )
    val lukketPeriode2 = TestRecord(
        kafkaKey2, lukketPeriode(
            id = periodeId2,
            identitetsnummer = identitetsnummer2,
            startet = metadata(tidspunkt = Instant.now().minus(Duration.ofHours(2))),
            avsluttet = metadata(tidspunkt = Instant.now())
        )
    )

    val bekreftelseTilgjengelig1a = TestRecord(
        kafkaKey1, bekreftelseTilgjengelig(
            periodeId = periodeId1,
            bekreftelseId = bekreftelseId1,
            arbeidssoekerId = arbeidssoekerId1,
            hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(50))
        )
    )
    val bekreftelseTilgjengelig1b = TestRecord(
        kafkaKey2, bekreftelseTilgjengelig(
            periodeId = periodeId1,
            bekreftelseId = bekreftelseId2,
            arbeidssoekerId = arbeidssoekerId1,
            hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(40))
        )
    )
    val bekreftelseTilgjengelig1c = TestRecord(
        kafkaKey2, bekreftelseTilgjengelig(
            periodeId = periodeId1,
            bekreftelseId = bekreftelseId3,
            arbeidssoekerId = arbeidssoekerId1,
            hendelseTidspunkt = Instant.now().minus(Duration.ofMinutes(30))
        )
    )
    val bekreftelseMeldingMottatt1 = TestRecord(
        kafkaKey1, bekreftelseMeldingMottatt(
            periodeId = periodeId1,
            bekreftelseId = bekreftelseId1,
            arbeidssoekerId = arbeidssoekerId1,
        )
    )
    val baOmAaAvsluttePeriode1 = TestRecord(
        kafkaKey1, baOmAaAvsluttePeriode(
            periodeId = periodeId1,
            arbeidssoekerId = arbeidssoekerId1
        )
    )
    val bekreftelsePaaVegneAvStartet1 = TestRecord(
        kafkaKey1, bekreftelsePaaVegneAvStartet(
            periodeId = periodeId1,
            arbeidssoekerId = arbeidssoekerId1
        )
    )
    val registerGracePeriodeUtloept1 = TestRecord(
        kafkaKey1, registerGracePeriodeUtloept(
            periodeId = periodeId1,
            arbeidssoekerId = arbeidssoekerId1,
            bekreftelseId = bekreftelseId1
        )
    )
    val registerGracePeriodeUtloeptEtterEksternInnsamling1 = TestRecord(
        kafkaKey1, registerGracePeriodeUtloeptEtterEksternInnsamling(
            periodeId = periodeId1,
            arbeidssoekerId = arbeidssoekerId1
        )
    )
    val registerGracePeriodeGjenstaaendeTid1 = TestRecord(
        kafkaKey1, registerGracePeriodeGjenstaaendeTid(
            periodeId = periodeId1,
            arbeidssoekerId = arbeidssoekerId1,
            bekreftelseId = bekreftelseId1
        )
    )
    val eksternGracePeriodeUtloept1 = TestRecord(
        kafkaKey1, eksternGracePeriodeUtloept(
            periodeId = periodeId1,
            arbeidssoekerId = arbeidssoekerId1
        )
    )
    val leveringsfristUtloept1 = TestRecord(
        kafkaKey1, leveringsfristUtloept(
            periodeId = periodeId1,
            arbeidssoekerId = arbeidssoekerId1
        )
    )
    val periodeAvsluttet1 = TestRecord(
        kafkaKey1, periodeAvsluttet(
            periodeId = periodeId1,
            arbeidssoekerId = arbeidssoekerId1
        )
    )

    val oppgaveVarselHendelse1a = TestRecord(
        bekreftelseId1.toString(), varselHendelse(
            eventName = VarselEventName.OPPRETTET,
            varselId = bekreftelseId1.toString(),
            varseltype = VarselType.OPPGAVE,
            tidspunkt = Instant.now().minus(Duration.ofMinutes(5))
        )
    )
    val oppgaveVarselHendelse1b = TestRecord(
        bekreftelseId1.toString(), varselHendelse(
            eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
            varselId = bekreftelseId1.toString(),
            status = VarselStatus.BESTILT,
            varseltype = VarselType.OPPGAVE,
            tidspunkt = Instant.now().minus(Duration.ofMinutes(4))
        )
    )
    val oppgaveVarselHendelse1c = TestRecord(
        bekreftelseId1.toString(), varselHendelse(
            eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
            varselId = bekreftelseId1.toString(),
            status = VarselStatus.SENDT,
            varseltype = VarselType.OPPGAVE,
            tidspunkt = Instant.now().minus(Duration.ofMinutes(3))
        )
    )
    val oppgaveVarselHendelse1d = TestRecord(
        bekreftelseId1.toString(), varselHendelse(
            eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
            varselId = bekreftelseId1.toString(),
            status = VarselStatus.FERDIGSTILT,
            varseltype = VarselType.OPPGAVE,
            tidspunkt = Instant.now().minus(Duration.ofMinutes(2))
        )
    )
    val oppgaveVarselHendelse1e = TestRecord(
        bekreftelseId1.toString(), varselHendelse(
            eventName = VarselEventName.INAKTIVERT,
            varselId = bekreftelseId1.toString(),
            varseltype = VarselType.OPPGAVE,
            tidspunkt = Instant.now().minus(Duration.ofMinutes(1))
        )
    )
    val oppgaveVarselHendelse2a = TestRecord(
        bekreftelseId2.toString(), varselHendelse(
            eventName = VarselEventName.OPPRETTET,
            varselId = bekreftelseId2.toString(),
            status = VarselStatus.VENTER,
            varseltype = VarselType.OPPGAVE,
            tidspunkt = Instant.now().minus(Duration.ofMinutes(3))
        )
    )
    val oppgaveVarselHendelse2b = TestRecord(
        bekreftelseId1.toString(), varselHendelse(
            eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
            varselId = bekreftelseId2.toString(),
            status = VarselStatus.BESTILT,
            varseltype = VarselType.OPPGAVE,
            tidspunkt = Instant.now().minus(Duration.ofMinutes(2))
        )
    )
    val oppgaveVarselHendelse2c = TestRecord(
        bekreftelseId1.toString(), varselHendelse(
            eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
            varselId = bekreftelseId2.toString(),
            status = VarselStatus.FEILET,
            varseltype = VarselType.OPPGAVE,
            tidspunkt = Instant.now().minus(Duration.ofMinutes(1))
        )
    )
    val oppgaveVarselHendelse2d = TestRecord(
        bekreftelseId1.toString(), varselHendelse(
            eventName = VarselEventName.INAKTIVERT,
            varselId = bekreftelseId2.toString(),
            varseltype = VarselType.OPPGAVE,
            tidspunkt = Instant.now().minus(Duration.ofMinutes(10))
        )
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
}