package no.nav.paw.aareg

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.JsonConvertException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ConnectException

/**
 * klient for å hente ut aktive arbeidsforhold på en person
 */

class AaregClient(
    private val url: String,
    private val getAccessToken: () -> String
) {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    private val aaregClientLogger: Logger = LoggerFactory.getLogger("paw-aareg-client")
    private val httpClient = createHttpClient()

    suspend fun hentArbeidsforhold(ident: String, callId: String): List<Arbeidsforhold> {
        val token = getAccessToken()
        try {
            val payload = httpClient.get(url) {
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                header("Nav-Call-Id", callId)
                header("Nav-Personident", ident)
            }.also {
                aaregClientLogger.info("Hentet arbeidsforhold fra aareg med status=$it.status()")
                sikkerLogg.debug("Svar fra aareg-API: " + it.bodyAsText())
            }.body<List<Arbeidsforhold>>()
            return payload
        } catch (responseException: ResponseException) {
            aaregClientLogger.error("Hente arbeidsforhold callId=[$callId] feilet med http-kode ${responseException.response.status}")
            throw responseException
        } catch (connectException: ConnectException) {
            aaregClientLogger.error("Hente arbeidsforhold callId=[$callId] feilet:", connectException)
            throw connectException
        } catch (jsonConvertException: JsonConvertException) {
            aaregClientLogger.error("Hente arbeidsforhold callId=[$callId] feilet, kunne ikke lese JSON")
            sikkerLogg.error("Hente arbeidsforhold callId=[$callId] feilet", jsonConvertException)
            throw jsonConvertException
        }
    }
}
