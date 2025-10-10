package no.nav.paw.kafkakeygenerator.test

import com.expediagroup.graphql.client.serialization.types.KotlinxGraphQLError
import com.expediagroup.graphql.client.serialization.types.KotlinxGraphQLResponse
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.AvvistStoppAvPeriode
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Kilde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.identitet.internehendelser.vo.IdentitetType.AKTORID
import no.nav.paw.identitet.internehendelser.vo.IdentitetType.FOLKEREGISTERIDENT
import no.nav.paw.identitet.internehendelser.vo.IdentitetType.NPID
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.model.KonfliktRow
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.test.TestData.asIdentGruppe
import no.nav.paw.pdl.PdlException
import no.nav.paw.pdl.graphql.generated.HentIdenter
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.pdl.graphql.generated.hentidenter.Identliste
import no.nav.paw.serialization.jackson.buildObjectMapper
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.Instant
import java.util.*

private val objectMapper = buildObjectMapper

object TestData {

    const val key1 = -100001L
    const val key2 = -100002L
    const val key3 = -100003L
    const val key4 = -100004L
    const val key5 = -100005L
    const val key6 = -100006L
    val dnr1 = Identitet(identitet = "41017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val dnr2 = Identitet(identitet = "42017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val dnr3 = Identitet(identitet = "43017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val dnr4 = Identitet(identitet = "44017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val dnr5 = Identitet(identitet = "45017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val dnr6 = Identitet(identitet = "46017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val dnr7 = Identitet(identitet = "47017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val dnr8_1 = Identitet(identitet = "48017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val dnr8_2 = Identitet(identitet = "48017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val dnr9 = Identitet(identitet = "49017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val dnr11 = Identitet(identitet = "51017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr1_1 = Identitet(identitet = "01017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr1_2 = Identitet(identitet = "01017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr2_1 = Identitet(identitet = "02017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr2_2 = Identitet(identitet = "02017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr3_1 = Identitet(identitet = "03017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr3_2 = Identitet(identitet = "03017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr4_1 = Identitet(identitet = "04017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr4_2 = Identitet(identitet = "04017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr5_1 = Identitet(identitet = "05017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr5_2 = Identitet(identitet = "05017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr6_1 = Identitet(identitet = "06017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr6_2 = Identitet(identitet = "06017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr7_1 = Identitet(identitet = "07017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr7_2 = Identitet(identitet = "07017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr8_1 = Identitet(identitet = "08017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr8_2 = Identitet(identitet = "08017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr9_1 = Identitet(identitet = "09017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr9_2 = Identitet(identitet = "09017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr10 = Identitet(identitet = "10017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr11_1 = Identitet(identitet = "11017012345", type = FOLKEREGISTERIDENT, gjeldende = true)
    val fnr11_2 = Identitet(identitet = "11017012346", type = FOLKEREGISTERIDENT, gjeldende = true)
    val aktorId1 = Identitet(identitet = "200001017012345", type = AKTORID, gjeldende = true)
    val aktorId2 = Identitet(identitet = "200002017012345", type = AKTORID, gjeldende = true)
    val aktorId3 = Identitet(identitet = "200003017012345", type = AKTORID, gjeldende = true)
    val aktorId4 = Identitet(identitet = "200004017012345", type = AKTORID, gjeldende = true)
    val aktorId5 = Identitet(identitet = "200005017012345", type = AKTORID, gjeldende = true)
    val aktorId6_1 = Identitet(identitet = "200006017012345", type = AKTORID, gjeldende = true)
    val aktorId6_2 = Identitet(identitet = "200006017012346", type = AKTORID, gjeldende = true)
    val aktorId7_1 = Identitet(identitet = "200007017012345", type = AKTORID, gjeldende = true)
    val aktorId7_2 = Identitet(identitet = "200007017012346", type = AKTORID, gjeldende = true)
    val aktorId8_1 = Identitet(identitet = "200008017012345", type = AKTORID, gjeldende = true)
    val aktorId8_2 = Identitet(identitet = "200008017012346", type = AKTORID, gjeldende = true)
    val aktorId9 = Identitet(identitet = "200009017012345", type = AKTORID, gjeldende = true)
    val aktorId10 = Identitet(identitet = "200010017012345", type = AKTORID, gjeldende = true)
    val aktorId11_1 = Identitet(identitet = "200011017012345", type = AKTORID, gjeldende = true)
    val aktorId11_2 = Identitet(identitet = "200011017012346", type = AKTORID, gjeldende = true)
    val npId1 = Identitet(identitet = "900001017012345", type = NPID, gjeldende = true)
    val npId2 = Identitet(identitet = "900002017012345", type = NPID, gjeldende = true)
    val npId3 = Identitet(identitet = "900003017012345", type = NPID, gjeldende = true)
    val npId4 = Identitet(identitet = "900004017012345", type = NPID, gjeldende = true)
    val npId5 = Identitet(identitet = "900005017012345", type = NPID, gjeldende = true)
    val npId6 = Identitet(identitet = "900006017012345", type = NPID, gjeldende = true)
    val npId7 = Identitet(identitet = "900007017012345", type = NPID, gjeldende = true)
    val npId8_1 = Identitet(identitet = "900008017012345", type = NPID, gjeldende = true)
    val npId8_2 = Identitet(identitet = "900008017012346", type = NPID, gjeldende = true)
    val npId9 = Identitet(identitet = "900009017012345", type = NPID, gjeldende = true)
    val npId10 = Identitet(identitet = "900010017012345", type = NPID, gjeldende = true)
    val npId11 = Identitet(identitet = "900010017012345", type = NPID, gjeldende = true)
    val periodeId1_1 = UUID.fromString("4c0cb50a-3b4a-45df-b5b6-2cb45f04d19b")
    val periodeId1_2 = UUID.fromString("095639e7-4240-42eb-813b-a1c003556e74")
    val periodeId2_1 = UUID.fromString("0fc3de47-a6cd-4ad5-8433-53235738200d")
    val periodeId2_2 = UUID.fromString("eb66d551-d91d-453d-bdc7-fc17443b3baa")
    val periodeId2_3 = UUID.fromString("e4dea660-a8ee-48c4-91a8-6fda081ab115")
    val periodeId3_1 = UUID.fromString("12cf8147-a76d-4b62-85d2-4792fea08995")
    val periodeId3_2 = UUID.fromString("883ae54d-1e18-41b1-992a-3a239b3d56f2")
    val periodeId3_3 = UUID.fromString("11644ba7-71bd-4817-b069-537fe058809c")
    val periodeId4_1 = UUID.fromString("f6f2f98a-2f2b-401d-b837-6ad26e45d4bf")
    val periodeId4_2 = UUID.fromString("f72ce5b4-0392-4534-92f4-153493f85c94")
    val periodeId4_3 = UUID.fromString("bf92ecd7-5141-4bcd-9ab5-e7a9321ac242")
    val periodeId5_1 = UUID.fromString("f6384bc5-a0ec-4bdc-9262-f6ebf952269f")
    val periodeId5_2 = UUID.fromString("7525c98b-2914-41b9-badb-7d87c5ba64a4")
    val periodeId6 = UUID.fromString("cc32b92a-1c3a-4522-bbeb-ccabc6a53c2a")
    val navIdent1 = "NAV0001"
    val navName1 = "Kari Normann"

    val aktor1_1 = aktor(
        listOf(aktorId1, npId1, dnr1)
            .map { it.asIdentifikator() }
    )
    val aktor1_2 = aktor(
        listOf(aktorId1, npId1, dnr1.copy(gjeldende = false), fnr1_1)
            .map { it.asIdentifikator() }
    )
    val aktor1_3 = aktor(
        listOf(aktorId1, npId1, dnr1.copy(gjeldende = false), fnr1_1.copy(gjeldende = false), fnr1_2)
            .map { it.asIdentifikator() }
    )
    val aktor2_1 = aktor(
        listOf(aktorId2, npId2, dnr2)
            .map { it.asIdentifikator() }
    )
    val aktor2_2 = aktor(
        listOf(aktorId2, npId2, dnr2.copy(gjeldende = false), fnr2_1)
            .map { it.asIdentifikator() }
    )
    val aktor2_3 = aktor(
        listOf(aktorId2, npId2, dnr2.copy(gjeldende = false), fnr2_1.copy(gjeldende = false), fnr2_2)
            .map { it.asIdentifikator() }
    )
    val aktor3_1 = aktor(
        listOf(aktorId3, npId3, dnr3)
            .map { it.asIdentifikator() }
    )
    val aktor3_2 = aktor(
        listOf(aktorId3, npId3, dnr3.copy(gjeldende = false), fnr3_1)
            .map { it.asIdentifikator() }
    )
    val aktor3_3 = aktor(
        listOf(aktorId3, npId3, dnr3.copy(gjeldende = false), fnr3_1.copy(gjeldende = false), fnr3_2)
            .map { it.asIdentifikator() }
    )
    val aktor4_1 = aktor(
        listOf(aktorId4, npId4, dnr4)
            .map { it.asIdentifikator() }
    )
    val aktor4_2 = aktor(
        listOf(aktorId4, npId4, dnr4.copy(gjeldende = false), fnr4_1)
            .map { it.asIdentifikator() }
    )
    val aktor4_3 = aktor(
        listOf(aktorId4, npId4, dnr4.copy(gjeldende = false), fnr4_2)
            .map { it.asIdentifikator() }
    )
    val aktor5_3 = aktor(
        listOf(
            aktorId5,
            dnr5.copy(gjeldende = false),
            fnr5_1.copy(gjeldende = false),
            fnr5_2
        ).map { it.asIdentifikator() }
    )
    val aktor7_1 = aktor(
        listOf(aktorId7_1, npId7, dnr7.copy(gjeldende = false), fnr7_1).map { it.asIdentifikator() }
    )
    val aktor7_2 = aktor(
        listOf(aktorId7_2, fnr7_1).map { it.asIdentifikator() }
    )
    val aktor8_1 = aktor(
        listOf(aktorId8_1, npId8_1, dnr8_1).map { it.asIdentifikator() }
    )
    val aktor8_2 = aktor(
        listOf(aktorId8_2, npId8_2, fnr8_1).map { it.asIdentifikator() }
    )
    val aktor8_3 = aktor(
        listOf(
            aktorId8_1.copy(gjeldende = false),
            aktorId8_2,
            npId8_1.copy(gjeldende = false),
            npId8_2,
            dnr8_1.copy(gjeldende = false),
            fnr8_1
        ).map { it.asIdentifikator() }
    )
    val aktor10 = aktor(
        listOf(aktorId10, npId10, fnr10).map { it.asIdentifikator() }
    )

    val periode1_1 = periodeStartet(
        periodeId = periodeId1_1,
        identitetsnummer = dnr1.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30)))
    )
    val periode2_1 = periodeAvsluttet(
        periodeId = periodeId2_1,
        identitetsnummer = dnr2.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(150))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(120)))
    )
    val periode2_2 = periodeAvsluttet(
        periodeId = periodeId2_2,
        identitetsnummer = fnr2_1.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(90))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(60)))
    )
    val periode2_3 = periodeStartet(
        periodeId = periodeId2_3,
        identitetsnummer = fnr2_2.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30)))
    )
    val periode3_1 = periodeStartet(
        periodeId = periodeId3_1,
        identitetsnummer = dnr3.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(60)))
    )
    val periode3_2 = periodeStartet(
        periodeId = periodeId3_2,
        identitetsnummer = fnr3_1.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30)))
    )
    val periode4_1 = periodeAvsluttet(
        periodeId = periodeId4_1,
        identitetsnummer = dnr4.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(120))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(90)))
    )
    val periode4_2 = periodeAvsluttet(
        periodeId = periodeId4_2,
        identitetsnummer = dnr4.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(60))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30)))
    )
    val periode5_1 = periodeAvsluttet(
        periodeId = periodeId5_1,
        identitetsnummer = dnr5.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(120))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(90)))
    )
    val periode5_2 = periodeAvsluttet(
        periodeId = periodeId5_2,
        identitetsnummer = fnr5_1.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(60))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30)))
    )
    val periode6_1 = periodeAvsluttet(
        periodeId = periodeId6,
        identitetsnummer = dnr6.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(120)))
    )
    val periode6_2 = periodeAvsluttet(
        periodeId = periodeId6,
        identitetsnummer = fnr6_1.identitet,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(120))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(90)))
    )

    fun brukerHendelse(): Bruker = Bruker(
        type = BrukerType.SYSTEM,
        id = "paw",
        sikkerhetsnivaa = null
    )

    fun metadataHendelse(): Metadata =
        Metadata(
            tidspunkt = Instant.now(),
            utfoertAv = brukerHendelse(),
            kilde = "paw",
            aarsak = "test"
        )

    fun periodeStartetHendelse(
        identitetsnummer: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): Startet = Startet(
        hendelseId = UUID.randomUUID(),
        id = arbeidssoekerId.value,
        identitetsnummer = identitetsnummer.value,
        metadata = metadataHendelse()
    )

    fun periodeAvsluttetHendelse(
        identitetsnummer: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): Avsluttet = Avsluttet(
        hendelseId = UUID.randomUUID(),
        id = arbeidssoekerId.value,
        identitetsnummer = identitetsnummer.value,
        metadata = metadataHendelse()
    )

    fun periodeStartAvvistHendelse(
        identitetsnummer: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): Avvist = Avvist(
        hendelseId = UUID.randomUUID(),
        id = arbeidssoekerId.value,
        identitetsnummer = identitetsnummer.value,
        metadata = metadataHendelse()
    )

    fun periodeAvsluttetAvvistHendelse(
        identitetsnummer: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): AvvistStoppAvPeriode = AvvistStoppAvPeriode(
        hendelseId = UUID.randomUUID(),
        id = arbeidssoekerId.value,
        identitetsnummer = identitetsnummer.value,
        metadata = metadataHendelse()
    )

    fun identitetsnummerSammenslaattHendelse(
        identitetsnummerList: List<Identitetsnummer>,
        fraArbeidssoekerId: ArbeidssoekerId,
        tilArbeidssoekerId: ArbeidssoekerId
    ): IdentitetsnummerSammenslaatt = IdentitetsnummerSammenslaatt(
        hendelseId = UUID.randomUUID(),
        id = fraArbeidssoekerId.value,
        identitetsnummer = identitetsnummerList.first().value,
        metadata = metadataHendelse(),
        flyttedeIdentitetsnumre = HashSet(identitetsnummerList.map { it.value }),
        flyttetTilArbeidssoekerId = tilArbeidssoekerId.value
    )

    fun arbeidssoekerIdFlettetInnHendelse(
        identitetsnummerList: List<Identitetsnummer>,
        tilArbeidssoekerId: ArbeidssoekerId,
        fraArbeidssoekerId: ArbeidssoekerId
    ): ArbeidssoekerIdFlettetInn = ArbeidssoekerIdFlettetInn(
        hendelseId = UUID.randomUUID(),
        id = tilArbeidssoekerId.value,
        identitetsnummer = identitetsnummerList.first().value,
        metadata = metadataHendelse(),
        kilde = Kilde(
            identitetsnummer = HashSet(identitetsnummerList.map { it.value }),
            arbeidssoekerId = fraArbeidssoekerId.value
        )
    )

    fun periodeBruker(
        type: no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType = no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.SYSTEM,
        id: String = "paw",
        sikkerhetsnivaa: String = "test:test"
    ): no.nav.paw.arbeidssokerregisteret.api.v1.Bruker =
        no.nav.paw.arbeidssokerregisteret.api.v1.Bruker(type, id, sikkerhetsnivaa)

    fun periodeTidspunktFraKilde(
        tidspunkt: Instant = Instant.now(),
        avviksType: AvviksType = AvviksType.UKJENT_VERDI
    ): TidspunktFraKilde = TidspunktFraKilde(tidspunkt, avviksType)

    fun periodeMetadata(
        tidspunkt: Instant = Instant.now(),
        utfoertAv: no.nav.paw.arbeidssokerregisteret.api.v1.Bruker = periodeBruker(),
        kilde: String = "paw",
        aarsak: String = "test",
        tidspunktFraKilde: TidspunktFraKilde? = null
    ): no.nav.paw.arbeidssokerregisteret.api.v1.Metadata =
        no.nav.paw.arbeidssokerregisteret.api.v1.Metadata(tidspunkt, utfoertAv, kilde, aarsak, tidspunktFraKilde)

    fun periodeStartet(
        periodeId: UUID = periodeId1_1,
        identitetsnummer: String = fnr1_1.identitet,
        startet: no.nav.paw.arbeidssokerregisteret.api.v1.Metadata = periodeMetadata()
    ): Periode = Periode(periodeId, identitetsnummer, startet, null)

    fun periodeAvsluttet(
        periodeId: UUID = periodeId1_1,
        identitetsnummer: String = fnr1_1.identitet,
        startet: no.nav.paw.arbeidssokerregisteret.api.v1.Metadata = periodeMetadata(
            tidspunkt = Instant.now().minus(Duration.ofDays(30))
        ),
        avsluttet: no.nav.paw.arbeidssokerregisteret.api.v1.Metadata = periodeMetadata()
    ): Periode = Periode(periodeId, identitetsnummer, startet, avsluttet)

    fun identifikator(
        ident: String = fnr1_1.identitet,
        type: Type = Type.FOLKEREGISTERIDENT,
        gjeldende: Boolean = true
    ): Identifikator = Identifikator(ident, type, gjeldende)

    fun aktor(
        identifikatorer: List<Identifikator> = aktor1_2.identifikatorer
    ): Aktor = Aktor(identifikatorer)

    fun <K, V> List<ConsumerRecord<K, V>>.asRecords(): ConsumerRecords<K, V> {
        val tp = TopicPartition(this.first().topic(), this.first().partition())
        return ConsumerRecords<K, V>(mapOf(tp to this), emptyMap())
    }

    fun Type.asIdentGruppe(): IdentGruppe = when (this) {
        Type.NPID -> IdentGruppe.NPID
        Type.AKTORID -> IdentGruppe.AKTORID
        Type.FOLKEREGISTERIDENT -> IdentGruppe.FOLKEREGISTERIDENT
    }

    fun Identifikator.asIdentInformasjon(): IdentInformasjon = IdentInformasjon(
        ident = this.idnummer,
        gruppe = this.type.asIdentGruppe(),
        historisk = !this.gjeldende
    )

    fun Aktor.asGraphQLResponse(): KotlinxGraphQLResponse<HentIdenter.Result> {
        return KotlinxGraphQLResponse(
            data = HentIdenter.Result(
                hentIdenter = Identliste(
                    identer = identifikatorer.map { it.asIdentInformasjon() }
                )
            ))
    }

    fun IdentitetType.asType(): Type = when (this) {
        NPID -> Type.NPID
        AKTORID -> Type.AKTORID
        FOLKEREGISTERIDENT -> Type.FOLKEREGISTERIDENT
        else -> throw IllegalArgumentException("Ukjent type")
    }

    fun IdentitetType.asIdentGruppe(): IdentGruppe = when (this) {
        NPID -> IdentGruppe.NPID
        AKTORID -> IdentGruppe.AKTORID
        FOLKEREGISTERIDENT -> IdentGruppe.FOLKEREGISTERIDENT
        else -> throw IllegalArgumentException("Ukjent type")
    }

    fun Identitet.asIdentifikator(): Identifikator = Identifikator(identitet, type.asType(), gjeldende)

    fun asPdlAktor(request: HentIdenter): Aktor {
        return when (request.variables.ident) {
            aktorId1.identitet -> aktor1_1
            npId1.identitet -> aktor1_1
            dnr1.identitet -> aktor1_1
            fnr1_1.identitet -> aktor1_3
            fnr1_2.identitet -> aktor1_3
            aktorId2.identitet -> aktor2_1
            npId2.identitet -> aktor2_1
            dnr2.identitet -> aktor2_1
            fnr2_1.identitet -> aktor2_2
            fnr2_2.identitet -> aktor2_3
            aktorId3.identitet -> aktor3_1
            npId3.identitet -> aktor3_1
            dnr3.identitet -> aktor3_1
            fnr3_1.identitet -> aktor3_2
            fnr3_2.identitet -> aktor3_3
            aktorId4.identitet -> aktor4_1
            dnr4.identitet -> aktor4_1
            fnr4_1.identitet -> aktor4_2
            fnr4_2.identitet -> aktor4_3
            dnr5.identitet -> aktor5_3
            else -> throw PdlException("Fant ikke identiteter", listOf(KotlinxGraphQLError("Fant ikke person")))
        }
    }

    fun KotlinxGraphQLResponse<*>.asString(): String = objectMapper.writeValueAsString(this)
}

fun Identitet.asIdentInformasjon(): IdentInformasjon = IdentInformasjon(
    ident = identitet,
    gruppe = type.asIdentGruppe(),
    historisk = !gjeldende
)

data class IdentitetWrapper(
    val arbeidssoekerId: Long,
    val aktorId: String,
    val identitet: Identitet,
    val status: IdentitetStatus
)

fun IdentitetRow.asWrapper(): IdentitetWrapper = IdentitetWrapper(
    arbeidssoekerId = arbeidssoekerId,
    aktorId = aktorId,
    identitet = Identitet(identitet, type, gjeldende),
    status = status
)

data class KonfliktWrapper(
    val aktorId: String,
    val type: KonfliktType,
    val status: KonfliktStatus,
    val identiteter: List<Identitet>
)

fun KonfliktRow.asWrapper(): KonfliktWrapper = KonfliktWrapper(
    aktorId = aktorId,
    type = type,
    status = status,
    identiteter = identiteter.map { it.asIdentitet() }
)
