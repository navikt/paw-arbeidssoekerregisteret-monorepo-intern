package no.nav.paw.bekreftelse.api.consumer

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.InnloggetBruker
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest

class BekreftelseHttpConsumer(private val httpClient: HttpClient) {

    suspend fun finnTilgjengeligBekreftelser(
        host: String,
        innloggetBruker: InnloggetBruker,
        request: TilgjengeligeBekreftelserRequest
    ): TilgjengeligBekreftelserResponse {
        val url = "http://$host/api/v1/tilgjengelige-rapporteringer"
        val response = httpClient.post(url) {
            bearerAuth(innloggetBruker.bearerToken)
            setBody(request)
        }
        // TODO Error handling
        return response.body()
    }

    suspend fun mottaBekreftelse(
        host: String,
        innloggetBruker: InnloggetBruker,
        request: BekreftelseRequest,
    ) {
        val url = "http://$host/api/v1/rapportering"
        val response = httpClient.post(url) {
            bearerAuth(innloggetBruker.bearerToken)
            setBody(request)
        }
        // TODO Error handling
    }
}