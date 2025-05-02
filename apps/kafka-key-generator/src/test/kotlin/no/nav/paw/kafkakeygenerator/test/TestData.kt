package no.nav.paw.kafkakeygenerator.test

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
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
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.time.Instant
import java.util.*
import kotlin.random.Random.Default.nextLong

const val person1_fødselsnummer = "01017012346"
const val person1_aktor_id = "2649500819544"
const val person1_dnummer = "09127821913"
const val person1_annen_ident = "12129127821913"
const val person2_fødselsnummer = "01017012345"
const val person2_aktor_id = "1649500819544"
const val person3_fødselsnummer = "01017012344"

fun hentSvar(ident: String) =
    when (ident) {
        person1_fødselsnummer -> person1MockSvar
        person1_aktor_id -> person1MockSvar
        person1_dnummer -> person1MockSvar
        person1_annen_ident -> person1MockSvar
        person2_fødselsnummer -> person2MockSvar
        person2_aktor_id -> person2MockSvar
        person3_fødselsnummer -> person3MockSvar
        else -> ingenTreffMockSvar
    }

const val ingenTreffMockSvar = """
{
  "data": {
    "hentIdenter": {
      "identer": []
    }
  }
}
"""
const val person1MockSvar = """
{
  "data": {
    "hentIdenter": {
      "identer": [
        {
          "ident": "$person1_fødselsnummer",
          "gruppe": "FOLKEREGISTERIDENT",
          "historisk": false
        },
        {
          "ident": "$person1_aktor_id",
          "gruppe": "AKTORID",
          "historisk": false
        },
        {
          "ident": "$person1_dnummer",
          "gruppe": "FOLKEREGISTERIDENT",
          "historisk": true
        },
        {
          "ident": "$person1_annen_ident",
          "gruppe": "ANNEN_IDENT",
          "historisk": true
        }
      ]
    }
  }
}
"""

const val person3MockSvar = """
    {
  "data": {
    "hentIdenter": {
      "identer": [
        {
          "ident": "$person3_fødselsnummer",
          "gruppe": "FOLKEREGISTERIDENT",
          "historisk": false
        }
      ]
    }
  }
}
"""

const val person2MockSvar = """
 {
  "data": {
    "hentIdenter": {
      "identer": [
        {
          "ident": "$person2_fødselsnummer",
          "gruppe": "FOLKEREGISTERIDENT",
          "historisk": false
        },
        {
          "ident": "$person2_aktor_id",
          "gruppe": "AKTORID",
          "historisk": false
        }
      ]
    }
  }
}   
"""

fun MockRequestHandleScope.genererResponse(it: HttpRequestData): HttpResponseData {
    val text = (it.body as TextContent).text
    val start = text.indexOf("ident")
    val end = text.indexOf("}", start)
    val ident = text
        .substring(start, end)
        .split(",")
        .first()
        .replace("\"", "")
        .replace("ident:", "")
        .trim()
    return respond(
        content = hentSvar(ident),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )
}

object TestData {

    const val fnr1 = "01017012345"
    const val fnr2 = "02017012345"
    const val fnr3 = "03017012345"
    const val fnr4 = "04017012345"
    const val fnr5 = "05017012345"
    const val dnr1 = "41017012345"
    const val dnr2 = "42017012345"
    const val dnr3 = "43017012345"
    const val dnr4 = "44017012345"
    const val dnr5 = "45017012345"
    const val aktorId1 = "200001017012345"
    const val aktorId2 = "200002017012345"
    const val aktorId3 = "200003017012345"
    const val aktorId4 = "200004017012345"
    const val aktorId5 = "200005017012345"
    const val npId1 = "900001017012345"
    const val npId2 = "900002017012345"
    const val npId3 = "900003017012345"
    const val npId4 = "900004017012345"
    const val npId5 = "900005017012345"

    val aktor1_1 = aktor(
        listOf(
            identifikator(ident = dnr1, type = Type.FOLKEREGISTERIDENT, gjeldende = true),
            identifikator(ident = aktorId1, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId1, type = Type.NPID, gjeldende = true)
        )
    )
    val aktor1_2 = aktor(
        listOf(
            identifikator(ident = fnr1, type = Type.FOLKEREGISTERIDENT, gjeldende = true),
            identifikator(ident = dnr1, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = aktorId1, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId1, type = Type.NPID, gjeldende = true)
        )
    )
    val aktor2_1 = aktor(
        listOf(
            identifikator(ident = dnr2, type = Type.FOLKEREGISTERIDENT, gjeldende = true),
            identifikator(ident = aktorId2, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId2, type = Type.NPID, gjeldende = true)
        )
    )
    val aktor2_2 = aktor(
        listOf(
            identifikator(ident = fnr2, type = Type.FOLKEREGISTERIDENT, gjeldende = true),
            identifikator(ident = dnr2, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = aktorId2, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId2, type = Type.NPID, gjeldende = true)
        )
    )
    val aktor3_1 = aktor(
        listOf(
            identifikator(ident = dnr3, type = Type.FOLKEREGISTERIDENT, gjeldende = true),
            identifikator(ident = aktorId3, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId3, type = Type.NPID, gjeldende = true)
        )
    )
    val aktor3_2 = aktor(
        listOf(
            identifikator(ident = fnr3, type = Type.FOLKEREGISTERIDENT, gjeldende = true),
            identifikator(ident = dnr3, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = aktorId3, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId3, type = Type.NPID, gjeldende = true)
        )
    )
    val aktor4_1 = aktor(
        listOf(
            identifikator(ident = dnr4, type = Type.FOLKEREGISTERIDENT, gjeldende = true),
            identifikator(ident = aktorId4, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId4, type = Type.NPID, gjeldende = true)
        )
    )
    val aktor4_2 = aktor(
        listOf(
            identifikator(ident = dnr4, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = fnr4, type = Type.FOLKEREGISTERIDENT, gjeldende = true),
            identifikator(ident = aktorId4, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId4, type = Type.NPID, gjeldende = true)
        )
    )
    val aktor5_1 = aktor(
        listOf(
            identifikator(ident = dnr5, type = Type.FOLKEREGISTERIDENT, gjeldende = false),
            identifikator(ident = aktorId5, type = Type.AKTORID, gjeldende = true),
            identifikator(ident = npId5, type = Type.NPID, gjeldende = true)
        )
    )


    fun bruker(): Bruker = Bruker(
        type = BrukerType.SYSTEM,
        id = "paw",
        sikkerhetsnivaa = null
    )

    fun metadata(): Metadata =
        Metadata(
            tidspunkt = Instant.now(),
            utfoertAv = bruker(),
            kilde = "paw",
            aarsak = "test"
        )

    fun periodeStartet(
        identitetsnummer: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): Startet = Startet(
        hendelseId = UUID.randomUUID(),
        id = arbeidssoekerId.value,
        identitetsnummer = identitetsnummer.value,
        metadata = metadata()
    )

    fun periodeAvsluttet(
        identitetsnummer: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): Avsluttet = Avsluttet(
        hendelseId = UUID.randomUUID(),
        id = arbeidssoekerId.value,
        identitetsnummer = identitetsnummer.value,
        metadata = metadata()
    )

    fun periodeStartAvvist(
        identitetsnummer: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): Avvist = Avvist(
        hendelseId = UUID.randomUUID(),
        id = arbeidssoekerId.value,
        identitetsnummer = identitetsnummer.value,
        metadata = metadata()
    )

    fun periodeAvsluttetAvvist(
        identitetsnummer: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): AvvistStoppAvPeriode = AvvistStoppAvPeriode(
        hendelseId = UUID.randomUUID(),
        id = arbeidssoekerId.value,
        identitetsnummer = identitetsnummer.value,
        metadata = metadata()
    )

    fun identitetsnummerSammenslaatt(
        identitetsnummerList: List<Identitetsnummer>,
        fraArbeidssoekerId: ArbeidssoekerId,
        tilArbeidssoekerId: ArbeidssoekerId
    ): IdentitetsnummerSammenslaatt = IdentitetsnummerSammenslaatt(
        hendelseId = UUID.randomUUID(),
        id = fraArbeidssoekerId.value,
        identitetsnummer = identitetsnummerList.first().value,
        metadata = metadata(),
        flyttedeIdentitetsnumre = HashSet(identitetsnummerList.map { it.value }),
        flyttetTilArbeidssoekerId = tilArbeidssoekerId.value
    )

    fun arbeidssoekerIdFlettetInn(
        identitetsnummerList: List<Identitetsnummer>,
        tilArbeidssoekerId: ArbeidssoekerId,
        fraArbeidssoekerId: ArbeidssoekerId
    ): ArbeidssoekerIdFlettetInn = ArbeidssoekerIdFlettetInn(
        hendelseId = UUID.randomUUID(),
        id = tilArbeidssoekerId.value,
        identitetsnummer = identitetsnummerList.first().value,
        metadata = metadata(),
        kilde = Kilde(
            identitetsnummer = HashSet(identitetsnummerList.map { it.value }),
            arbeidssoekerId = fraArbeidssoekerId.value
        )
    )

    fun identifikator(
        ident: String = fnr1,
        type: Type = Type.FOLKEREGISTERIDENT,
        gjeldende: Boolean = true
    ): Identifikator = Identifikator(ident, type, gjeldende)

    fun aktor(
        identifikatorer: List<Identifikator> = listOf(
            identifikator(ident = fnr1, type = Type.FOLKEREGISTERIDENT, gjeldende = true),
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

    data class IdentitetWrapper(
        val arbeidssoekerId: Long,
        val aktorId: String,
        val identitet: String,
        val type: IdentitetType,
        val gjeldende: Boolean,
        val status: IdentitetStatus
    )

    fun IdentitetRow.asWrapper(): IdentitetWrapper = IdentitetWrapper(
        arbeidssoekerId = arbeidssoekerId,
        aktorId = aktorId,
        identitet = identitet,
        type = type,
        gjeldende = gjeldende,
        status = status
    )
}