package no.nav.paw.arbeidssoeker.synk.test

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import no.nav.paw.arbeidssoeker.synk.config.JobConfig
import no.nav.paw.arbeidssoeker.synk.model.OpprettPeriodeRequest
import no.nav.paw.arbeidssoeker.synk.model.asOpprettPeriodeFeilType
import no.nav.paw.arbeidssoeker.synk.model.asOpprettPeriodeTilstand
import no.nav.paw.serialization.jackson.configureJackson

private val httpHeaders = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
fun buildMockHttpClient(
    jobConfig: JobConfig,
    responseMapping: Map<String, Pair<HttpStatusCode, String>>
) = HttpClient(MockEngine) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
    engine {
        addHandler { request ->
            val body = request.body.readValue<OpprettPeriodeRequest>()

            with(body) {
                periodeTilstand shouldBe jobConfig.defaultVerdier.periodeTilstand.asOpprettPeriodeTilstand()
                registreringForhaandsGodkjentAvAnsatt shouldBe jobConfig.defaultVerdier.forhaandsgodkjentAvAnsatt
                if (feilretting != null) {
                    feilretting?.feilType shouldBe jobConfig.defaultVerdier.feilrettingFeiltype.asOpprettPeriodeFeilType()
                    feilretting?.melding shouldBe jobConfig.defaultVerdier.feilrettingMelding
                    feilretting?.tidspunkt shouldNotBe null
                }
                val response = responseMapping[identitetsnummer]
                if (response != null) {
                    respond(response.second, response.first, httpHeaders)
                } else {
                    respond(byteArrayOf(), HttpStatusCode.NoContent, headersOf())
                }
            }
        }
    }
}
