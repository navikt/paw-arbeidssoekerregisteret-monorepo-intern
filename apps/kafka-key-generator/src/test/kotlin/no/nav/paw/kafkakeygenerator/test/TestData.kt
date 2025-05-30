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
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
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

fun List<Hendelse>.asConsumerRecords(): ConsumerRecords<Long, Hendelse> =
    this.map { TestData.getConsumerRecord(nextLong(), it) }
        .let { TestData.getConsumerRecords(it) }

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

    fun <K, V> getConsumerRecord(key: K, value: V): ConsumerRecord<K, V> =
        ConsumerRecord("topic", 1, 1, key, value)

    fun <K, V> getConsumerRecords(records: List<ConsumerRecord<K, V>>): ConsumerRecords<K, V> =
        ConsumerRecords(mapOf(TopicPartition("topic", 1) to records))
}