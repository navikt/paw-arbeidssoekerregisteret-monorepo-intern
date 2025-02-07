package no.nav.paw.dolly.api.test

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import no.nav.paw.dolly.api.model.BeskrivelseMedDetaljerResponse
import no.nav.paw.dolly.api.model.BrukerResponse
import no.nav.paw.dolly.api.model.BrukerType
import no.nav.paw.dolly.api.model.JaNeiVetIkke
import no.nav.paw.dolly.api.model.JobbSituasjonBeskrivelse
import no.nav.paw.dolly.api.model.MetadataResponse
import no.nav.paw.dolly.api.model.OpplysningerOmArbeidssoekerAggregertResponse
import no.nav.paw.dolly.api.model.OppslagResponse
import no.nav.paw.dolly.api.model.ProfileringResponse
import no.nav.paw.dolly.api.model.ProfileringsResultat
import no.nav.paw.dolly.api.model.UtdanningResponse
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest
import no.nav.paw.dolly.api.models.Brukertype
import no.nav.paw.dolly.api.models.Jobbsituasjonsbeskrivelse
import no.nav.paw.dolly.api.models.Jobbsituasjonsdetaljer
import java.time.Instant
import java.util.*

inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
    headers {
        append(HttpHeaders.ContentType, ContentType.Application.Json)
    }
    setBody(body)
}

object TestData {

    fun nyArbeidssoekerregistreringRequest(identitetsnummer: String) =
        ArbeidssoekerregistreringRequest(
            identitetsnummer = identitetsnummer,
        )

    fun fullstendingArbeidssoekerregistreringRequest(identitetsnummer: String) =
        ArbeidssoekerregistreringRequest(
            identitetsnummer = identitetsnummer,
            utfoertAv = Brukertype.SLUTTBRUKER,
            kilde = "Dolly",
            aarsak = "Registrering av arbeidssøker i Dolly",
            nuskode = "3",
            utdanningBestaatt = true,
            utdanningGodkjent = true,
            jobbsituasjonsbeskrivelse = Jobbsituasjonsbeskrivelse.HAR_BLITT_SAGT_OPP,
            jobbsituasjonsdetaljer = Jobbsituasjonsdetaljer(stillingStyrk08 = "00", stillingstittel = "Annen stilling"),
            helsetilstandHindrerArbeid = false,
            andreForholdHindrerArbeid = false
        )

    fun oppslagsApiResponse(): OppslagResponse = OppslagResponse(
        periodeId = UUID.randomUUID(),
        startet = MetadataResponse(
            tidspunkt = Instant.now(),
            utfoertAv = BrukerResponse(
                type = BrukerType.SLUTTBRUKER,
                id = "test"
            ),
            kilde = "Dolly",
            aarsak = "Registrering av arbeidssøker i Dolly"
        ),
        opplysningerOmArbeidssoeker = listOf(
            OpplysningerOmArbeidssoekerAggregertResponse(
                opplysningerOmArbeidssoekerId = UUID.randomUUID(),
                periodeId = UUID.randomUUID(),
                sendtInnAv = MetadataResponse(
                    tidspunkt = Instant.now(),
                    utfoertAv = BrukerResponse(
                        type = BrukerType.SLUTTBRUKER,
                        id = "test"
                    ),
                    kilde = "Dolly",
                    aarsak = "Registrering av arbeidssøker i Dolly"
                ),
                jobbsituasjon = listOf(
                    BeskrivelseMedDetaljerResponse(
                        beskrivelse = JobbSituasjonBeskrivelse.HAR_BLITT_SAGT_OPP,
                        detaljer = mapOf("stillingStyrk08" to "00", "stilling" to "Annen stilling")
                    )
                ),
                utdanning = UtdanningResponse(
                    nus = "3",
                    bestaatt = JaNeiVetIkke.JA,
                    godkjent = JaNeiVetIkke.JA
                ),
                helse = null,
                annet = null,
                profilering = ProfileringResponse(
                    profileringId = UUID.randomUUID(),
                    periodeId = UUID.randomUUID(),
                    opplysningerOmArbeidssoekerId = UUID.randomUUID(),
                    sendtInnAv = MetadataResponse(
                        tidspunkt = Instant.now(),
                        utfoertAv = BrukerResponse(
                            type = BrukerType.SLUTTBRUKER,
                            id = "test"
                        ),
                        kilde = "Dolly",
                        aarsak = "Registrering av arbeidssøker i Dolly"
                    ),
                    profilertTil = ProfileringsResultat.ANTATT_GODE_MULIGHETER,
                    jobbetSammenhengendeSeksAvTolvSisteManeder = true,
                    alder = 30
                )
            )
        ),
        bekreftelser = emptyList()
    )

}