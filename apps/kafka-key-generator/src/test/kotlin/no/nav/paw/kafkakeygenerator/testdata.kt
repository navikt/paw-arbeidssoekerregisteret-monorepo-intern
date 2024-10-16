package no.nav.paw.kafkakeygenerator

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*

const val person1_fødselsnummer = "01017012346"
const val person1_aktor_id = "2649500819544"
const val person1_dnummer = "09127821913"
const val person1_annen_ident = "12129127821913"
const val person2_fødselsnummer = "01017012345"
const val person2_aktor_id = "1649500819544"

fun hentSvar(ident: String) =
    when(ident) {
        person1_fødselsnummer -> person1MockSvar
        person1_aktor_id -> person1MockSvar
        person1_dnummer -> person1MockSvar
        person1_annen_ident -> person1MockSvar
        person2_fødselsnummer -> person2MockSvar
        person2_aktor_id -> person2MockSvar
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
          "gruppe": "FOLKEREGISTERIDENT"
        },
        {
          "ident": "$person1_aktor_id",
          "gruppe": "AKTORID"
        },
        {
          "ident": "$person1_dnummer",
          "gruppe": "FOLKEREGISTERIDENT"
        },
        {
          "ident": "$person1_annen_ident",
          "gruppe": "ANNEN_IDENT"
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
          "gruppe": "FOLKEREGISTERIDENT"
        },
        {
          "ident": "$person2_aktor_id",
          "gruppe": "AKTORID"
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
