package no.nav.paw.arbeidssoekerregisteret.test

import no.nav.paw.arbeidssoekerregisteret.model.BestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.BestillingStatus
import no.nav.paw.arbeidssoekerregisteret.model.BestillingerTable
import no.nav.paw.arbeidssoekerregisteret.model.BestiltVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.BestiltVarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.BestilteVarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.EksterneVarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.EksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.InsertBestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.InsertBestiltVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.InsertEksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.InsertPeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.InsertVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.UpdateBestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.UpdateBestiltVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.UpdatePeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.UpdateVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselKanal
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselRow
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.model.VarslerTable
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
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.random.Random
import no.nav.paw.bekreftelse.internehendelser.vo.Bruker as InternBekreftelseBruker

object TestData {
    val runtimeEnvironment = currentRuntimeEnvironment

    val key1 = -10001L
    val key2 = -10002L
    val key3 = -10003L
    val key4 = -10004L
    val key5 = -10005L
    val identitetsnummer1 = "01017012345"
    val identitetsnummer2 = "02017012345"
    val identitetsnummer3 = "03017012345"
    val identitetsnummer4 = "04017012345"
    val identitetsnummer5 = "05017012345"
    val arbeidssoekerId1 = 10001L
    val arbeidssoekerId2 = 10002L
    val arbeidssoekerId3 = 10003L
    val arbeidssoekerId4 = 10004L
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

    fun insertPeriodeRow(
        periodeId: UUID = UUID.randomUUID(),
        identitetsnummer: String = randomFnr(),
        startetTimestamp: Instant = Instant.now()
    ): InsertPeriodeRow = InsertPeriodeRow(
        periodeId = periodeId,
        identitetsnummer = identitetsnummer,
        startetTimestamp = startetTimestamp
    )

    fun updatePeriodeRow(
        periodeId: UUID = UUID.randomUUID(),
        identitetsnummer: String = randomFnr(),
        avsluttetTimestamp: Instant = Instant.now()
    ): UpdatePeriodeRow = UpdatePeriodeRow(
        periodeId = periodeId,
        identitetsnummer = identitetsnummer,
        avsluttetTimestamp = avsluttetTimestamp
    )

    fun varselRow(
        periodeId: UUID = UUID.randomUUID(),
        varselId: UUID = UUID.randomUUID(),
        varselKilde: VarselKilde = VarselKilde.UKJENT,
        varselType: VarselType = VarselType.UKJENT,
        varselStatus: VarselStatus = VarselStatus.UKJENT,
        hendelseName: VarselEventName = VarselEventName.UKJENT,
        hendelseTimestamp: Instant = Instant.now(),
        insertedTimestamp: Instant = Instant.now(),
        updatedTimestamp: Instant? = null
    ): VarselRow = VarselRow(
        periodeId = periodeId,
        varselId = varselId,
        varselKilde = varselKilde,
        varselType = varselType,
        varselStatus = varselStatus,
        hendelseName = hendelseName,
        hendelseTimestamp = hendelseTimestamp,
        insertedTimestamp = insertedTimestamp,
        updatedTimestamp = updatedTimestamp
    )

    fun insertVarselRow(
        periodeId: UUID = UUID.randomUUID(),
        varselId: UUID = UUID.randomUUID(),
        varselKilde: VarselKilde = VarselKilde.UKJENT,
        varselType: VarselType = VarselType.UKJENT,
        varselStatus: VarselStatus = VarselStatus.UKJENT,
        hendelseName: VarselEventName = VarselEventName.UKJENT,
        hendelseTimestamp: Instant = Instant.now()
    ): InsertVarselRow = InsertVarselRow(
        periodeId = periodeId,
        varselId = varselId,
        varselKilde = varselKilde,
        varselType = varselType,
        varselStatus = varselStatus,
        hendelseName = hendelseName,
        hendelseTimestamp = hendelseTimestamp
    )

    fun updateVarselRow(
        varselId: UUID = UUID.randomUUID(),
        varselStatus: VarselStatus = VarselStatus.UKJENT,
        hendelseName: VarselEventName = VarselEventName.UKJENT,
        hendelseTimestamp: Instant = Instant.now()
    ): UpdateVarselRow = UpdateVarselRow(
        varselId = varselId,
        varselStatus = varselStatus,
        hendelseName = hendelseName,
        hendelseTimestamp = hendelseTimestamp
    )

    fun eksterntVarselRow(
        varselId: UUID = UUID.randomUUID(),
        varselType: VarselType = VarselType.UKJENT,
        varselStatus: VarselStatus = VarselStatus.UKJENT,
        hendelseName: VarselEventName = VarselEventName.UKJENT,
        hendelseTimestamp: Instant = Instant.now(),
        insertedTimestamp: Instant = Instant.now(),
        updatedTimestamp: Instant? = null
    ): EksterntVarselRow = EksterntVarselRow(
        varselId = varselId,
        varselType = varselType,
        varselStatus = varselStatus,
        hendelseName = hendelseName,
        hendelseTimestamp = hendelseTimestamp,
        insertedTimestamp = insertedTimestamp,
        updatedTimestamp = updatedTimestamp
    )

    fun insertEksterntVarselRow(
        varselId: UUID = UUID.randomUUID(),
        varselType: VarselType = VarselType.UKJENT,
        varselStatus: VarselStatus = VarselStatus.UKJENT,
        hendelseName: VarselEventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
        hendelseTimestamp: Instant = Instant.now()
    ): InsertEksterntVarselRow = InsertEksterntVarselRow(
        varselId = varselId,
        varselType = varselType,
        varselStatus = varselStatus,
        hendelseName = hendelseName,
        hendelseTimestamp = hendelseTimestamp
    )

    fun insertBestillingRow(
        bestillingId: UUID = UUID.randomUUID(),
        bestiller: String = "NAV1234"
    ): InsertBestillingRow = InsertBestillingRow(
        bestillingId = bestillingId,
        bestiller = bestiller
    )

    fun updateBestillingRow(
        bestillingId: UUID = UUID.randomUUID(),
        status: BestillingStatus = BestillingStatus.BEKREFTET
    ): UpdateBestillingRow = UpdateBestillingRow(
        bestillingId = bestillingId,
        status = status
    )

    fun insertBestiltVarselRow(
        bestillingId: UUID = UUID.randomUUID(),
        periodeId: UUID = UUID.randomUUID(),
        varselId: UUID = UUID.randomUUID(),
        identitetsnummer: String = randomFnr()
    ): InsertBestiltVarselRow = InsertBestiltVarselRow(
        bestillingId = bestillingId,
        periodeId = periodeId,
        varselId = varselId,
        identitetsnummer = identitetsnummer
    )

    fun updateBestiltVarselRow(
        varselId: UUID = UUID.randomUUID(),
        status: BestiltVarselStatus = BestiltVarselStatus.AKTIV
    ): UpdateBestiltVarselRow = UpdateBestiltVarselRow(
        varselId = varselId,
        status = status
    )

    fun bruker(
        type: BrukerType = BrukerType.SYSTEM,
        id: String = "test",
        sikkerhetsnivaa: String? = null
    ): Bruker = Bruker(type, id, sikkerhetsnivaa)

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
        id: UUID = UUID.randomUUID(),
        identitetsnummer: String = randomFnr(),
        startet: Metadata = metadata()
    ): Periode = Periode(id, identitetsnummer, startet, null)

    fun lukketPeriode(
        id: UUID = UUID.randomUUID(),
        identitetsnummer: String = randomFnr(),
        startet: Metadata = metadata(tidspunkt = Instant.now().minus(Duration.ofDays(30))),
        avsluttet: Metadata = metadata(),
    ): Periode = Periode(id, identitetsnummer, startet, avsluttet)

    fun bekreftelseTilgjengelig(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = UUID.randomUUID(),
        bekreftelseId: UUID = UUID.randomUUID(),
        arbeidssoekerId: Long = Random.nextLong(),
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
        periodeId: UUID = UUID.randomUUID(),
        arbeidssoekerId: Long = Random.nextLong(),
        hendelseTidspunkt: Instant = Instant.now(),
        bekreftelseId: UUID = UUID.randomUUID(),
    ): BekreftelseMeldingMottatt = BekreftelseMeldingMottatt(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        bekreftelseId = bekreftelseId,
    )

    fun baOmAaAvsluttePeriode(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = UUID.randomUUID(),
        arbeidssoekerId: Long = Random.nextLong(),
        hendelseTidspunkt: Instant = Instant.now(),
        utfoertAv: InternBekreftelseBruker = InternBekreftelseBruker(
            type = no.nav.paw.bekreftelse.internehendelser.vo.BrukerType.SYSTEM,
            id = "test",
            sikkerhetsnivaa = null
        )
    ): BaOmAaAvsluttePeriode = BaOmAaAvsluttePeriode(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        utfoertAv = utfoertAv
    )

    fun bekreftelsePaaVegneAvStartet(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = UUID.randomUUID(),
        arbeidssoekerId: Long = Random.nextLong(),
        hendelseTidspunkt: Instant = Instant.now()
    ): BekreftelsePaaVegneAvStartet = BekreftelsePaaVegneAvStartet(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt
    )

    fun leveringsfristUtloept(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = UUID.randomUUID(),
        arbeidssoekerId: Long = Random.nextLong(),
        hendelseTidspunkt: Instant = Instant.now(),
        bekreftelseId: UUID = UUID.randomUUID(),
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
        periodeId: UUID = UUID.randomUUID(),
        arbeidssoekerId: Long = Random.nextLong(),
        hendelseTidspunkt: Instant = Instant.now(),
        bekreftelseId: UUID = UUID.randomUUID()
    ): RegisterGracePeriodeUtloept = RegisterGracePeriodeUtloept(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        bekreftelseId = bekreftelseId
    )

    fun registerGracePeriodeUtloeptEtterEksternInnsamling(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = UUID.randomUUID(),
        arbeidssoekerId: Long = Random.nextLong(),
        hendelseTidspunkt: Instant = Instant.now()
    ): RegisterGracePeriodeUtloeptEtterEksternInnsamling = RegisterGracePeriodeUtloeptEtterEksternInnsamling(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt
    )

    fun registerGracePeriodeGjenstaaendeTid(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = UUID.randomUUID(),
        arbeidssoekerId: Long = Random.nextLong(),
        hendelseTidspunkt: Instant = Instant.now(),
        bekreftelseId: UUID = UUID.randomUUID(),
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
        periodeId: UUID = UUID.randomUUID(),
        arbeidssoekerId: Long = Random.nextLong(),
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
        periodeId: UUID = UUID.randomUUID(),
        arbeidssoekerId: Long = Random.nextLong(),
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
        varselId: String = UUID.randomUUID().toString(),
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

    fun Periode.asRecord(key: Long = Random.nextLong()): TestRecord<Long, Periode> =
        TestRecord(key, this)

    fun BekreftelseTilgjengelig.asRecord(key: Long = Random.nextLong()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun BekreftelseMeldingMottatt.asRecord(key: Long = Random.nextLong()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun PeriodeAvsluttet.asRecord(key: Long = Random.nextLong()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun BaOmAaAvsluttePeriode.asRecord(key: Long = Random.nextLong()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun LeveringsfristUtloept.asRecord(key: Long = Random.nextLong()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun RegisterGracePeriodeUtloept.asRecord(key: Long = Random.nextLong()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun RegisterGracePeriodeUtloeptEtterEksternInnsamling.asRecord(key: Long = Random.nextLong()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun RegisterGracePeriodeGjenstaaendeTid.asRecord(key: Long = Random.nextLong()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun BekreftelsePaaVegneAvStartet.asRecord(key: Long = Random.nextLong()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun EksternGracePeriodeUtloept.asRecord(key: Long = Random.nextLong()): TestRecord<Long, BekreftelseHendelse> =
        TestRecord(key, this)

    fun VarselHendelse.asRecord(): TestRecord<String, VarselHendelse> =
        TestRecord(this.varselId, this)

    fun VarselRow.insert(): Int {
        val row = this
        return transaction {
            VarslerTable.insert {
                it[periodeId] = row.periodeId
                it[bekreftelseId] = row.bekreftelseId
                it[varselId] = row.varselId
                it[varselKilde] = row.varselKilde
                it[varselType] = row.varselType
                it[varselStatus] = row.varselStatus
                it[hendelseNavn] = row.hendelseName
                it[hendelseTimestamp] = row.hendelseTimestamp
                it[insertedTimestamp] = row.insertedTimestamp
                it[updatedTimestamp] = row.updatedTimestamp
            }.insertedCount
        }
    }

    fun EksterntVarselRow.insert(): Int {
        val row = this
        return transaction {
            EksterneVarslerTable.insert {
                it[varselId] = row.varselId
                it[varselType] = row.varselType
                it[varselStatus] = row.varselStatus
                it[hendelseNavn] = row.hendelseName
                it[hendelseTimestamp] = row.hendelseTimestamp
                it[insertedTimestamp] = row.insertedTimestamp
                it[updatedTimestamp] = row.updatedTimestamp
            }.insertedCount
        }
    }

    fun BestillingRow.insert(): Int {
        val row = this
        return transaction {
            BestillingerTable.insert {
                it[bestillingId] = row.bestillingId
                it[bestiller] = row.bestiller
                it[status] = row.status
                it[insertedTimestamp] = row.insertedTimestamp
                it[updatedTimestamp] = row.updatedTimestamp
            }.insertedCount
        }
    }

    fun BestiltVarselRow.insert(): Int {
        val row = this
        return transaction {
            BestilteVarslerTable.insert {
                it[bestillingId] = row.bestillingId
                it[periodeId] = row.periodeId
                it[varselId] = row.varselId
                it[identitetsnummer] = row.identitetsnummer
                it[status] = row.status
                it[insertedTimestamp] = row.insertedTimestamp
                it[updatedTimestamp] = row.updatedTimestamp
            }.insertedCount
        }
    }
}