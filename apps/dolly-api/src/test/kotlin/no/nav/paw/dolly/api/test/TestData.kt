package no.nav.paw.dolly.api.test

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest
import no.nav.paw.dolly.api.models.Brukertype
import no.nav.paw.dolly.api.models.Jobbsituasjonsbeskrivelse
import no.nav.paw.dolly.api.models.Jobbsituasjonsdetaljer

inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
    headers {
        append(HttpHeaders.ContentType, ContentType.Application.Json)
    }
    setBody(body)
}

object TestData {

    fun nyArbeidssoekerregistreringRequest() =
        ArbeidssoekerregistreringRequest(
            identitetsnummer = "12345678911",
        )

    fun fullstendingArbeidssoekerregistreringRequest() =
        ArbeidssoekerregistreringRequest(
            identitetsnummer = "12345678912",
            utfoertAv = Brukertype.SLUTTBRUKER,
            kilde = "Dolly",
            aarsak = "Registrering av arbeidssøker i Dolly",
            nuskode = "3",
            utdanningBestaatt = true,
            utdanningGodkjent = true,
            jobbsituasjonsbeskrivelse = Jobbsituasjonsbeskrivelse.HAR_BLITT_SAGT_OPP,
            jobbsituasjonsdetaljer = Jobbsituasjonsdetaljer(stillingStyrk08 = "00", stilling = "Annen stilling"),
            helsetilstandHindrerArbeid = false,
            andreForholdHindrerArbeid = false
        )

}