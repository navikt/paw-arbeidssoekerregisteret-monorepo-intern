package no.nav.paw.kafkakeygenerator.test

import com.expediagroup.graphql.client.serialization.types.KotlinxGraphQLResponse
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.AvvistStoppAvPeriode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Kilde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentitetHendelseDeserializer
import no.nav.paw.identitet.internehendelser.IdentitetHendelseSerializer
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterMergetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterSlettetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterSplittetHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.model.HendelseRow
import no.nav.paw.kafkakeygenerator.model.HendelseStatus
import no.nav.paw.kafkakeygenerator.model.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktRow
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.test.TestData.asResponse
import no.nav.paw.kafkakeygenerator.test.TestData.asString
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
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
import kotlin.random.Random.Default.nextLong

private val objectMapper = buildObjectMapper
private val hendelseSerializer: IdentitetHendelseSerializer = IdentitetHendelseSerializer()
private val hendelseDeserializer: IdentitetHendelseDeserializer = IdentitetHendelseDeserializer()

fun MockRequestHandleScope.genererResponse(it: HttpRequestData): HttpResponseData {
    val body = (it.body as TextContent).text
    val request = objectMapper.readValue<HentIdenter>(body)
    return respond(
        content = request.asResponse().asString(),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )
}

object TestData {

    const val key1 = -100001L
    const val key2 = -100002L
    const val key3 = -100003L
    const val key4 = -100004L
    const val key5 = -100005L
    const val key6 = -100006L
    const val key7 = -100007L
    const val key8 = -100008L
    const val key9 = -100009L
    const val fnr1_1 = "01017012345"
    const val fnr1_2 = "01017012346"
    const val fnr2_1 = "02017012345"
    const val fnr2_2 = "02017012346"
    const val fnr3_1 = "03017012345"
    const val fnr3_2 = "03017012346"
    const val fnr4_1 = "04017012345"
    const val fnr4_2 = "04017012346"
    const val fnr5 = "05017012345"
    const val fnr6 = "06017012345"
    const val fnr7 = "07017012345"
    const val fnr8 = "08017012345"
    const val fnr9 = "09017012345"
    const val fnr10 = "10017012345"
    const val dnr1 = "41017012345"
    const val dnr2 = "42017012345"
    const val dnr3 = "43017012345"
    const val dnr4 = "44017012345"
    const val dnr5 = "45017012345"
    const val dnr6 = "46017012345"
    const val dnr7 = "47017012345"
    const val dnr8 = "48017012345"
    const val dnr9 = "49017012345"
    const val aktorId1 = "200001017012345"
    const val aktorId2 = "200002017012345"
    const val aktorId3 = "200003017012345"
    const val aktorId4 = "200004017012345"
    const val aktorId5 = "200005017012345"
    const val aktorId6 = "200006017012345"
    const val aktorId7_1 = "200007017012345"
    const val aktorId7_2 = "200007017012346"
    const val aktorId8_1 = "200008017012345"
    const val aktorId8_2 = "200008017012346"
    const val aktorId9 = "200009017012345"
    const val aktorId10 = "200010017012345"
    const val npId1 = "900001017012345"
    const val npId2 = "900002017012345"
    const val npId3 = "900003017012345"
    const val npId4 = "900004017012345"
    const val npId5 = "900005017012345"
    const val npId6 = "900006017012345"
    const val npId7 = "900007017012345"
    const val npId8_1 = "900008017012345"
    const val npId8_2 = "900008017012346"
    const val npId9 = "900009017012345"
    const val npId10 = "900010017012345"
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
        listOf(
            identifikator(ident = aktorId1, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId1, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr1, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor1_2 = aktor(
        listOf(
            identifikator(ident = aktorId1, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId1, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr1, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = fnr1_1, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor2_1 = aktor(
        listOf(
            identifikator(ident = aktorId2, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId2, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr2, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor2_2 = aktor(
        listOf(
            identifikator(ident = aktorId2, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId2, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr2, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = fnr2_1, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor3_1 = aktor(
        listOf(
            identifikator(ident = aktorId3, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId3, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr3, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor3_2 = aktor(
        listOf(
            identifikator(ident = aktorId3, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId3, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr3, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = fnr3_1, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = fnr3_2, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor3_3 = aktor(
        listOf(
            identifikator(ident = aktorId3, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId3, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr3, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = fnr3_2, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor4_1 = aktor(
        listOf(
            identifikator(ident = aktorId4, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId4, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr4, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor4_2 = aktor(
        listOf(
            identifikator(ident = aktorId4, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId4, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr4, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = fnr4_1, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor7_1 = aktor(
        listOf(
            identifikator(ident = aktorId7_1, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId7, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr7, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = fnr7, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor7_2 = aktor(
        listOf(
            identifikator(ident = aktorId7_2, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = fnr7, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor8_1 = aktor(
        listOf(
            identifikator(ident = aktorId8_1, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId8_1, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr8, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor8_2 = aktor(
        listOf(
            identifikator(ident = aktorId8_2, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId8_2, type = Type.NPID, gjeldende = true),
            identifikator(ident = fnr8, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor8_3 = aktor(
        listOf(
            identifikator(ident = aktorId8_1, type = Type.AKTORID, gjeldende = false),
            identifikator(ident = aktorId8_2, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId8_1, type = Type.NPID, gjeldende = false),
            identifikator(ident = npId8_2, type = Type.NPID, gjeldende = true),
            identifikator(ident = dnr8, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = fnr8, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )
    val aktor10 = aktor(
        listOf(
            identifikator(ident = aktorId10, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId10, type = Type.NPID, gjeldende = true),
            identifikator(ident = fnr10, type = Type.FOLKEREGISTERIDENT, gjeldende = true)
        )
    )

    val periode1_1 = periodeStartet(
        periodeId = periodeId1_1,
        identitetsnummer = dnr1,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30)))
    )
    val periode2_1 = periodeAvsluttet(
        periodeId = periodeId2_1,
        identitetsnummer = dnr2,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(150))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(120)))
    )
    val periode2_2 = periodeAvsluttet(
        periodeId = periodeId2_2,
        identitetsnummer = fnr2_1,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(90))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(60)))
    )
    val periode2_3 = periodeStartet(
        periodeId = periodeId2_3,
        identitetsnummer = fnr2_2,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30)))
    )
    val periode3_1 = periodeStartet(
        periodeId = periodeId3_1,
        identitetsnummer = dnr3,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(60)))
    )
    val periode3_2 = periodeStartet(
        periodeId = periodeId3_2,
        identitetsnummer = fnr3_1,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30)))
    )
    val periode4_1 = periodeAvsluttet(
        periodeId = periodeId4_1,
        identitetsnummer = dnr4,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(120))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(90)))
    )
    val periode4_2 = periodeAvsluttet(
        periodeId = periodeId4_2,
        identitetsnummer = dnr4,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(60))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30)))
    )
    val periode5_1 = periodeAvsluttet(
        periodeId = periodeId5_1,
        identitetsnummer = dnr5,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(120))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(90)))
    )
    val periode5_2 = periodeAvsluttet(
        periodeId = periodeId5_2,
        identitetsnummer = fnr5,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(60))),
        avsluttet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(30)))
    )
    val periode6_1 = periodeAvsluttet(
        periodeId = periodeId6,
        identitetsnummer = dnr6,
        startet = periodeMetadata(tidspunkt = Instant.now().minus(Duration.ofDays(120)))
    )
    val periode6_2 = periodeAvsluttet(
        periodeId = periodeId6,
        identitetsnummer = fnr6,
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
        identitetsnummer: String = fnr1_1,
        startet: no.nav.paw.arbeidssokerregisteret.api.v1.Metadata = periodeMetadata()
    ): Periode = Periode(periodeId, identitetsnummer, startet, null)

    fun periodeAvsluttet(
        periodeId: UUID = periodeId1_1,
        identitetsnummer: String = fnr1_1,
        startet: no.nav.paw.arbeidssokerregisteret.api.v1.Metadata = periodeMetadata(
            tidspunkt = Instant.now().minus(Duration.ofDays(30))
        ),
        avsluttet: no.nav.paw.arbeidssokerregisteret.api.v1.Metadata = periodeMetadata()
    ): Periode = Periode(periodeId, identitetsnummer, startet, avsluttet)

    fun identifikator(
        ident: String = fnr1_1,
        type: Type = Type.FOLKEREGISTERIDENT,
        gjeldende: Boolean = true
    ): Identifikator = Identifikator(ident, type, gjeldende)

    fun aktor(
        identifikatorer: List<Identifikator> = listOf(
            identifikator(ident = fnr1_1, type = Type.FOLKEREGISTERIDENT, gjeldende = true),
            identifikator(ident = dnr1, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = aktorId1, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId1, type = Type.NPID, gjeldende = true)
        )
    ): Aktor = Aktor(identifikatorer)

    fun <K, V> List<ConsumerRecord<K, V>>.asRecords(): ConsumerRecords<K, V> {
        val tp = TopicPartition(this.first().topic(), this.first().partition())
        return ConsumerRecords<K, V>(mapOf(tp to this))
    }

    fun List<Hendelse>.asHendelseRecords(): ConsumerRecords<Long, Hendelse> =
        this.map { ConsumerRecord("topic", 0, 0, nextLong(), it) }
            .asRecords()

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

    fun HentIdenter.asResponse(): KotlinxGraphQLResponse<HentIdenter.Result> = when (variables.ident) {
        dnr1 -> aktor1_1.asGraphQLResponse()
        fnr1_1 -> aktor1_1.asGraphQLResponse()
        fnr1_2 -> aktor1_1.asGraphQLResponse()
        aktorId1 -> aktor1_1.asGraphQLResponse()
        dnr2 -> aktor2_1.asGraphQLResponse()
        fnr2_1 -> aktor2_1.asGraphQLResponse()
        fnr2_2 -> aktor2_1.asGraphQLResponse()
        aktorId2 -> aktor2_1.asGraphQLResponse()
        dnr3 -> aktor3_1.asGraphQLResponse()
        fnr3_1 -> aktor3_1.asGraphQLResponse()
        fnr3_2 -> aktor3_1.asGraphQLResponse()
        aktorId3 -> aktor3_1.asGraphQLResponse()
        dnr4 -> aktor4_1.asGraphQLResponse()
        fnr4_1 -> aktor4_2.asGraphQLResponse()
        else -> aktor(identifikatorer = emptyList()).asGraphQLResponse()
    }

    fun KotlinxGraphQLResponse<*>.asString(): String = objectMapper.writeValueAsString(this)

    fun Identitet.asIdentitetsnummer(): Identitetsnummer = Identitetsnummer(identitet)
}

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

data class IdentitetHendelseWrapper(
    val type: String,
    val identiteter: List<Identitet>,
    val tidligereIdentiteter: List<Identitet> = emptyList(),
)

fun IdentitetHendelse.asWrapper(): IdentitetHendelseWrapper {
    return when (this) {
        is IdentiteterEndretHendelse -> IdentitetHendelseWrapper(
            type = hendelseType,
            identiteter = identiteter,
            tidligereIdentiteter = tidligereIdentiteter,
        )

        is IdentiteterMergetHendelse -> IdentitetHendelseWrapper(
            type = hendelseType,
            identiteter = identiteter,
            tidligereIdentiteter = tidligereIdentiteter,
        )

        is IdentiteterSplittetHendelse -> IdentitetHendelseWrapper(
            type = hendelseType,
            identiteter = identiteter,
            tidligereIdentiteter = tidligereIdentiteter,
        )

        is IdentiteterSlettetHendelse -> IdentitetHendelseWrapper(
            type = hendelseType,
            identiteter = emptyList(),
            tidligereIdentiteter = tidligereIdentiteter,
        )
    }
}

data class HendelseWrapper(
    val arbeidssoekerId: Long? = null,
    val aktorId: String,
    val hendelse: IdentitetHendelseWrapper,
    val status: HendelseStatus
)

fun HendelseRow.asWrapper(): HendelseWrapper = HendelseWrapper(
    arbeidssoekerId = arbeidssoekerId,
    aktorId = aktorId,
    hendelse = hendelseDeserializer.deserializeFromString(data).asWrapper(),
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
