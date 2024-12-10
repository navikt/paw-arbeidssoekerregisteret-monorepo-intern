package no.nav.paw.tilgangskontroll.poaotilgang

import io.ktor.client.HttpClient
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.tilgangskontroll.TilgangsTjenesteForAnsatte
import java.net.URI

fun initPoaobackend(
    m2mTokenClient: AzureAdMachineToMachineTokenClient,
    httpClient: HttpClient,
    poaoConfig: PoaoConfig
): TilgangsTjenesteForAnsatte {
    return PoaoTilgangsTjeneste(
        httpClient = httpClient,
        poaTilgangUrl = URI.create(poaoConfig.url),
        poaoToken = { m2mTokenClient.createMachineToMachineToken(poaoConfig.scope) }
    )
}