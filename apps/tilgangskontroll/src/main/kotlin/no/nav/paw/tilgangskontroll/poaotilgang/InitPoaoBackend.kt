package no.nav.paw.tilgangskontroll.poaotilgang

import io.ktor.client.HttpClient
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.tilgangskontroll.SecureLogger
import no.nav.paw.tilgangskontroll.TilgangsTjenesteForAnsatte
import java.net.URI

fun initPoaobackend(
    secureLogger: SecureLogger,
    m2mTokenClient: AzureAdMachineToMachineTokenClient,
    httpClient: HttpClient,
    poaoConfig: PoaoConfig
): TilgangsTjenesteForAnsatte {
    return PoaoTilgangsTjeneste(
        secureLogger = secureLogger,
        httpClient = httpClient,
        poaTilgangUrl = URI.create(poaoConfig.url),
        poaoToken = { m2mTokenClient.createMachineToMachineToken(poaoConfig.scope) }
    )
}